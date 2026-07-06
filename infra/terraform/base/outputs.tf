output "network_id" {
  value = yandex_vpc_network.iot.id
}

output "subnet_id" {
  value = yandex_vpc_subnet.iot.id
}

output "zone" {
  value = var.zone
}

output "subnet_cidr" {
  value = var.subnet_cidr
}
