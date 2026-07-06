terraform {
  required_version = ">= 0.13"

  required_providers {
    yandex = {
      source  = "yandex-cloud/yandex"
      version = "0.209.0"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.3"
    }
  }

  backend "s3" {
    endpoints = {
      s3 = "https://storage.yandexcloud.net"
    }
    bucket = "iot-state-terraform"
    region = "ru-central1"
    key    = "vm/terraform.tfstate"

    skip_region_validation      = true
    skip_credentials_validation = true
    skip_requesting_account_id  = true
    skip_s3_checksum            = true
  }
}

provider "yandex" {
  service_account_key_file = var.yc_service_account_key_file
  cloud_id                 = var.yc_cloud_id
  folder_id                = var.yc_folder_id
  zone                     = data.terraform_remote_state.base.outputs.zone
}

data "terraform_remote_state" "base" {
  backend = "s3"
  config = {
    endpoints = { s3 = "https://storage.yandexcloud.net" }
    bucket    = "iot-state-terraform"
    region    = "ru-central1"
    key       = "base/terraform.tfstate"

    skip_region_validation      = true
    skip_credentials_validation = true
    skip_requesting_account_id  = true
    skip_s3_checksum            = true
  }
}

data "yandex_compute_image" "ubuntu" {
  family = var.image_family
}

resource "yandex_vpc_security_group" "iot" {
  name       = "${var.vm_name}-sg"
  network_id = data.terraform_remote_state.base.outputs.network_id

  dynamic "ingress" {
    for_each = var.open_tcp_ports
    content {
      description    = "Allow TCP ${ingress.value}"
      protocol       = "TCP"
      v4_cidr_blocks = var.allowed_cidrs
      port           = ingress.value
    }
  }

  egress {
    description    = "Allow all outbound traffic"
    protocol       = "ANY"
    v4_cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "yandex_compute_instance" "iot_vm" {
  name        = var.vm_name
  platform_id = var.platform_id
  zone        = data.terraform_remote_state.base.outputs.zone

  labels = {
    environment = var.environment
    project     = "iot-service"
    managed-by  = "terraform"
  }

  resources {
    cores         = var.cores
    memory        = var.memory
    core_fraction = var.core_fraction
  }

  boot_disk {
    initialize_params {
      image_id = data.yandex_compute_image.ubuntu.id
      size     = var.disk_size
      type     = "network-ssd"
    }
  }

  network_interface {
    subnet_id          = data.terraform_remote_state.base.outputs.subnet_id
    nat                = true
    ip_address         = var.vm_internal_ip
    security_group_ids = [yandex_vpc_security_group.iot.id]
  }

  metadata = {
    "ssh-keys" = "${var.ssh_user}:${var.ssh_public_key}"
  }
}

# =============================================================================
# Backup storage
# =============================================================================

resource "yandex_iam_service_account" "storage_sa" {
  name        = "iot-storage-sa"
  description = "Service account for Yandex Object Storage backup management"
}

resource "yandex_resourcemanager_folder_iam_member" "storage_sa_admin" {
  folder_id = var.yc_folder_id
  role      = "storage.admin"
  member    = "serviceAccount:${yandex_iam_service_account.storage_sa.id}"
}

resource "yandex_resourcemanager_folder_iam_member" "storage_sa_kms" {
  folder_id = var.yc_folder_id
  role      = "kms.keys.encrypterDecrypter"
  member    = "serviceAccount:${yandex_iam_service_account.storage_sa.id}"
}

resource "yandex_iam_service_account_static_access_key" "storage_sa_static_key" {
  service_account_id = yandex_iam_service_account.storage_sa.id
  description        = "Static access key for S3 bucket operations"
}

resource "random_string" "bucket_suffix" {
  length  = 16
  special = false
  upper   = false
}

resource "yandex_kms_symmetric_key" "backup_key" {
  name              = "backup-encryption-key"
  description       = "KMS key for encrypting backups in Object Storage"
  default_algorithm = "AES_256"
  rotation_period   = "8760h"
}

resource "yandex_storage_bucket" "backup_bucket" {
  bucket = "${var.backup_bucket_name}-${random_string.bucket_suffix.result}"

  depends_on = [
    yandex_iam_service_account_static_access_key.storage_sa_static_key,
    yandex_resourcemanager_folder_iam_member.storage_sa_admin,
    yandex_resourcemanager_folder_iam_member.storage_sa_kms,
    yandex_kms_symmetric_key.backup_key
  ]

  versioning {
    enabled = true
  }

  lifecycle_rule {
    id      = "cleanup-old-backups"
    enabled = true

    expiration {
      days = var.backup_retention_days
    }

    noncurrent_version_expiration {
      days = 7
    }
  }

  server_side_encryption_configuration {
    rule {
      apply_server_side_encryption_by_default {
        kms_master_key_id = yandex_kms_symmetric_key.backup_key.id
        sse_algorithm     = "aws:kms"
      }
    }
  }
}

resource "yandex_storage_bucket_grant" "backup_bucket_grant" {
  access_key = yandex_iam_service_account_static_access_key.storage_sa_static_key.access_key
  secret_key = yandex_iam_service_account_static_access_key.storage_sa_static_key.secret_key
  bucket     = yandex_storage_bucket.backup_bucket.bucket

  grant {
    id          = yandex_iam_service_account.storage_sa.id
    type        = "CanonicalUser"
    permissions = ["FULL_CONTROL"]
  }

  depends_on = [
    yandex_storage_bucket.backup_bucket,
    yandex_resourcemanager_folder_iam_member.storage_sa_kms,
  ]
}
