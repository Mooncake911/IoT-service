terraform {
  required_version = ">= 0.13"

  required_providers {
    yandex = {
      source  = "yandex-cloud/yandex"
      version = "0.209.0"
    }
  }

  backend "s3" {
    endpoints = {
      s3 = "https://storage.yandexcloud.net"
    }
    bucket = "iot-state-terraform"
    region = "ru-central1"
    key    = "k8s/terraform.tfstate"

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

# Service account for K8s cluster management
resource "yandex_iam_service_account" "k8s_sa" {
  name        = "${var.k8s_cluster_name}-sa"
  description = "Service account for Managed Kubernetes cluster"
}

resource "yandex_resourcemanager_folder_iam_member" "k8s_sa_agent" {
  folder_id = var.yc_folder_id
  role      = "k8s.clusters.agent"
  member    = "serviceAccount:${yandex_iam_service_account.k8s_sa.id}"
}

resource "yandex_resourcemanager_folder_iam_member" "k8s_sa_puller" {
  folder_id = var.yc_folder_id
  role      = "container-registry.images.puller"
  member    = "serviceAccount:${yandex_iam_service_account.k8s_sa.id}"
}

resource "yandex_resourcemanager_folder_iam_member" "k8s_sa_editor" {
  folder_id = var.yc_folder_id
  role      = "editor"
  member    = "serviceAccount:${yandex_iam_service_account.k8s_sa.id}"
}

# Security group for K8s nodes
resource "yandex_vpc_security_group" "k8s_node_sg" {
  name       = "${var.k8s_cluster_name}-node-sg"
  network_id = data.terraform_remote_state.base.outputs.network_id

  ingress {
    description    = "Allow all internal traffic"
    protocol       = "ANY"
    v4_cidr_blocks = [data.terraform_remote_state.base.outputs.subnet_cidr]
  }

  ingress {
    description    = "Allow HTTPS from internet for Ingress"
    protocol       = "TCP"
    port           = 443
    v4_cidr_blocks = var.allowed_cidrs
  }

  egress {
    description    = "Allow all outbound traffic"
    protocol       = "ANY"
    v4_cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "yandex_kubernetes_cluster" "iot" {
  name        = var.k8s_cluster_name
  description = "IoT-service Managed Kubernetes cluster"
  network_id  = data.terraform_remote_state.base.outputs.network_id

  master {
    version   = var.k8s_version
    public_ip = true
    zonal {
      zone      = data.terraform_remote_state.base.outputs.zone
      subnet_id = data.terraform_remote_state.base.outputs.subnet_id
    }
    security_group_ids = [yandex_vpc_security_group.k8s_node_sg.id]
  }

  service_account_id      = yandex_iam_service_account.k8s_sa.id
  node_service_account_id = yandex_iam_service_account.k8s_sa.id

  release_channel = var.k8s_release_channel

  depends_on = [
    yandex_resourcemanager_folder_iam_member.k8s_sa_agent,
    yandex_resourcemanager_folder_iam_member.k8s_sa_puller,
    yandex_resourcemanager_folder_iam_member.k8s_sa_editor,
  ]
}

resource "yandex_kubernetes_node_group" "default" {
  name        = "default"
  description = "Default node group"
  cluster_id  = yandex_kubernetes_cluster.iot.id

  instance_template {
    platform_id = var.platform_id

    resources {
      cores         = var.k8s_node_cores
      memory        = var.k8s_node_memory
      core_fraction = var.k8s_node_core_fraction
    }

    boot_disk {
      size = var.k8s_node_disk
      type = "network-ssd"
    }

    network_interface {
      nat                = true
      subnet_ids         = [data.terraform_remote_state.base.outputs.subnet_id]
      security_group_ids = [yandex_vpc_security_group.k8s_node_sg.id]
    }
  }

  scale_policy {
    fixed_scale {
      size = var.k8s_node_count
    }
  }

  allocation_policy {
    location {
      zone = data.terraform_remote_state.base.outputs.zone
    }
  }
}
