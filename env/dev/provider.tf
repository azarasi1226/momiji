terraform {
  required_version = "~> 1.5.6"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.15.0"
    }
  }
}

provider "aws" {
  region  = var.aws_region
  profile = var.aws_profile
}