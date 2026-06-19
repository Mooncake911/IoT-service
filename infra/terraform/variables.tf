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

variable "vm_name" {
  type        = string
  description = "Virtual machine name"
  default     = "iot-vm"
}

variable "ssh_user" {
  type        = string
  description = "SSH user created on the VM"
  default     = "ubuntu"
}

variable "ssh_public_key" {
  type        = string
  description = "Public SSH key content for VM access"
}

variable "image_family" {
  type        = string
  description = "Ubuntu image family"
  default     = "ubuntu-2404-lts-oslogin"
}

variable "disk_size" {
  type        = number
  description = "Boot disk size in GB"
  default     = 20
}

variable "platform_id" {
  type        = string
  description = "Compute platform"
  default     = "standard-v3"
}

variable "cores" {
  type        = number
  description = "CPU cores"
  default     = 4
}

variable "memory" {
  type        = number
  description = "RAM in GB"
  default     = 8
}

variable "core_fraction" {
  type        = number
  description = "CPU core fraction"
  default     = 100
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

variable "open_tcp_ports" {
  type        = list(number)
  description = "TCP ports exposed from the VM"
  default     = [22, 8081, 8082, 8083, 8084, 8085, 8501, 5672, 15672, 27017, 27018, 27019]
}

variable "allowed_cidrs" {
  type        = list(string)
  description = "CIDR blocks allowed to access the VM"
  default     = ["0.0.0.0/0"]
}

variable "environment" {
  type        = string
  description = "Deployment environment"
  default     = "production"
}

variable "backup_bucket_name" {
  type        = string
  description = "Name of the Yandex Object Storage bucket for cold storage and backups"
  default     = "iot-cold-storage"
}

variable "backup_retention_days" {
  type        = number
  description = "Number of days to keep backups in Object Storage before expiration"
  default     = 7
}
