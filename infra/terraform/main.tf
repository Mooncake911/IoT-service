data "yandex_compute_image" "ubuntu" {
  family = var.image_family
}

resource "yandex_vpc_network" "iot" {
  name = var.network_name
}

resource "yandex_vpc_subnet" "iot" {
  name           = "${var.network_name}-${var.zone}"
  zone           = var.zone
  network_id     = yandex_vpc_network.iot.id
  v4_cidr_blocks = [var.subnet_cidr]
}

resource "yandex_vpc_security_group" "iot" {
  name       = "${var.vm_name}-sg"
  network_id = yandex_vpc_network.iot.id

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
  zone        = var.zone

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
    subnet_id          = yandex_vpc_subnet.iot.id
    nat                = true
    security_group_ids = [yandex_vpc_security_group.iot.id]
  }

  metadata = {
    "ssh-keys" = "${var.ssh_user}:${var.ssh_public_key}"
  }
}

# Service Account for managing backups and cold storage
resource "yandex_iam_service_account" "storage_sa" {
  name        = "iot-storage-sa"
  description = "Service account for Yandex Object Storage backup management"
}

# Grant storage.admin role to the service account
resource "yandex_resourcemanager_folder_iam_member" "storage_sa_admin" {
  folder_id = var.yc_folder_id
  role      = "storage.admin"
  member    = "serviceAccount:${yandex_iam_service_account.storage_sa.id}"
}

# Grant KMS encrypter/decrypter role — required to read/write objects in
# KMS-encrypted buckets. Without this the S3 write will be rejected even if
# the SA has FULL_CONTROL on the bucket ACL.
resource "yandex_resourcemanager_folder_iam_member" "storage_sa_kms" {
  folder_id = var.yc_folder_id
  role      = "kms.keys.encrypterDecrypter"
  member    = "serviceAccount:${yandex_iam_service_account.storage_sa.id}"
}

# Generate static access key for the storage service account
resource "yandex_iam_service_account_static_access_key" "storage_sa_static_key" {
  service_account_id = yandex_iam_service_account.storage_sa.id
  description        = "Static access key for S3 bucket operations"
}

# Generate a random suffix for the S3 bucket to ensure global uniqueness
resource "random_string" "bucket_suffix" {
  length  = 16
  special = false
  upper   = false
}

# Generate a backup encryption key
resource "yandex_kms_symmetric_key" "backup_key" {
  name              = "backup-encryption-key"
  description       = "KMS key for encrypting backups in Object Storage"
  default_algorithm = "AES_256"
  rotation_period   = "8760h" # Per year (365 days) rotation
}

# Create Yandex Object Storage bucket for backup
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

# Grant the storage SA full S3-level access to the bucket.
# Yandex Object Storage uses S3 ACLs for static-key requests — folder-level
# IAM roles (storage.admin) are NOT propagated to S3 ACL checks, so without
# this grant the MinIO mc client will get "Insufficient permissions".
# `grant` was extracted from yandex_storage_bucket to yandex_storage_bucket_grant
# as required by newer versions of the Yandex Terraform provider.
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
