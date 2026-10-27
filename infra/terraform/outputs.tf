output "ecr_repository_url" {
  value = aws_ecr_repository.app.repository_url
}

output "load_balancer_dns" {
  value = aws_lb.api.dns_name
}

output "cloudfront_domain" {
  value = aws_cloudfront_distribution.web.domain_name
}

output "web_bucket" {
  value = aws_s3_bucket.web.bucket
}

output "kafka_bootstrap_tls" {
  value = aws_msk_cluster.kafka.bootstrap_brokers_tls
}
