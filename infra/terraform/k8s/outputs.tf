output "k8s_cluster_id" {
  value = yandex_kubernetes_cluster.iot.id
}

output "k8s_cluster_name" {
  value = yandex_kubernetes_cluster.iot.name
}

output "k8s_master_endpoint" {
  value       = yandex_kubernetes_cluster.iot.master[0].external_v4_endpoint
  description = "External endpoint for the K8s API"
}
