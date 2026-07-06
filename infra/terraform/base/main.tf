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
    key    = "base/terraform.tfstate"

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
  zone                     = var.zone
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
