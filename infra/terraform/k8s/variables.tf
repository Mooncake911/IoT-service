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

variable "k8s_cluster_name" {
  type        = string
  description = "Managed Kubernetes cluster name"
  default     = "iot-k8s"
}

variable "k8s_version" {
  type        = string
  description = "Kubernetes version"
  default     = "1.35"
}

variable "k8s_release_channel" {
  type        = string
  description = "Release channel: RAPID, REGULAR, STABLE"
  default     = "REGULAR"
}

variable "k8s_node_count" {
  type        = number
  description = "Number of K8s worker nodes"
  default     = 2
}

variable "k8s_node_cores" {
  type        = number
  description = "CPU cores per K8s node"
  default     = 2
}

variable "k8s_node_memory" {
  type        = number
  description = "RAM in GB per K8s node"
  default     = 4
}

variable "k8s_node_disk" {
  type        = number
  description = "Boot disk size in GB per K8s node"
  default     = 32
}

variable "k8s_node_core_fraction" {
  type        = number
  description = "CPU core fraction for K8s nodes"
  default     = 100
}

variable "platform_id" {
  type        = string
  description = "Compute platform"
  default     = "standard-v3"
}

variable "allowed_cidrs" {
  type        = list(string)
  description = "CIDR blocks allowed to access the K8s API"
  default     = ["0.0.0.0/0"]
}
