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

variable "vm_name" {
  type        = string
  description = "Virtual machine name"
  default     = "iot-vm"
}

variable "vm_internal_ip" {
  type        = string
  description = "Static internal IP for the DB VM (from the subnet CIDR)"
  default     = "10.10.0.100"
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

variable "open_tcp_ports" {
  type        = list(number)
  description = "Public TCP ports exposed from the VM (SSH is separate, see allowed_admin_cidrs). NOTE: observability (Grafana 3000, Prometheus 9090, Kibana 5601) is intentionally NOT exposed — it is served via the dashboard UI reverse-proxy (<ui>:8501/grafana/, <ui>:8501/prometheus/), prod-like."
  default     = [8081, 8082, 8083, 8084, 8085, 8501, 5672, 15672, 27017, 27018, 27019]
}

variable "allowed_public_cidrs" {
  type        = list(string)
  description = "CIDR blocks allowed to reach public app/DB ports (narrow in prod)"
  default     = ["0.0.0.0/0"]
}

variable "allowed_admin_cidrs" {
  type        = list(string)
  description = "CIDR blocks allowed to reach SSH port 22 (narrow to your IP in prod)"
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
