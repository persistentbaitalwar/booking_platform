variable "region" {
  type    = string
  default = "ap-south-1"
}

variable "name" {
  type    = string
  default = "movie-booking"
}

variable "environment" {
  type    = string
  default = "prod"
}

variable "image_tag" {
  description = "Tag of the backend image pushed to the ECR repository created here"
  type        = string
  default     = "latest"
}

variable "api_certificate_arn" {
  description = "ACM certificate (same region) for the API domain used by the load balancer"
  type        = string
}

variable "api_domain" {
  description = "DNS name that points to the load balancer (a Route 53 alias you create); CloudFront forwards /api/* to it"
  type        = string
}

variable "task_count" {
  type    = number
  default = 3
}

variable "allowed_cors_origin" {
  description = "Origin of the web app, for example https://tickets.example.com"
  type        = string
}
