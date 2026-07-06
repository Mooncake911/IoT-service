variable "yc_service_account_key_file" {
  type        = string
  description = "Path to the service account key JSON file"
}

variable "yc_cloud_id" {
  type        = string
  description = "Yandex Cloud ID"
}

variable "yc_folder_id" {
  type        = string
  description = "Yandex Cloud folder ID"
}

variable "zone" {
  type        = string
  description = "Default compute zone"
  default     = "ru-central1-a"
}

variable "network_name" {
  type        = string
  description = "VPC network name"
  default     = "iot-network"
}

variable "subnet_cidr" {
  type        = string
  description = "Subnet CIDR"
  default     = "10.10.0.0/24"
}
