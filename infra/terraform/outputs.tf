output "vm_name" {
  value = yandex_compute_instance.iot_vm.name
}

output "vm_id" {
  value = yandex_compute_instance.iot_vm.id
}

output "public_ip" {
  value = yandex_compute_instance.iot_vm.network_interface[0].nat_ip_address
}

output "ssh_command" {
  value = "ssh ${var.ssh_user}@${yandex_compute_instance.iot_vm.network_interface[0].nat_ip_address}"
}

output "backup_bucket_name" {
  value       = yandex_storage_bucket.backup_bucket.bucket
  description = "The name of the Object Storage bucket created for backups"
}

output "backup_storage_access_key" {
  value       = yandex_iam_service_account_static_access_key.storage_sa_static_key.access_key
  description = "The access key ID for the backup storage service account"
}

output "backup_storage_secret_key" {
  value       = yandex_iam_service_account_static_access_key.storage_sa_static_key.secret_key
  description = "The secret access key for the backup storage service account"
  sensitive   = true
}

