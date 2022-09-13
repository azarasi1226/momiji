# HTTP用セキュリティグループ
module "http_sg" {
    source = "../security_group"

    resource_prefix = var.resource_prefix
    usage_name = "http"
    vpc_id = var.vpc_id
    port = 80
    cidr_blocks = ["0.0.0.0/0"]
}

# HTTPS用セキュリティグループ
module "https_sg" {
    source = "../security_group"

    resource_prefix = var.resource_prefix
    usage_name = "https"
    vpc_id = var.vpc_id
    port = 443
    cidr_blocks = ["0.0.0.0/0"]
}

# テスト用セキュリティグループ(Gree/Blue)
module "test_sg" {
    source = "../security_group"

    resource_prefix = var.resource_prefix
    usage_name = "test"
    vpc_id = var.vpc_id
    port = 80
    cidr_blocks = ["0.0.0.0/0"]
}

# ロードバランサー
resource "aws_lb" "this" {
    name = "${var.resource_prefix}-${var.service_name}-alb"
    load_balancer_type = "application"
    internal = false

    subnets = var.subnet_ids
    security_groups = [
        module.http_sg.security_group_id,
        module.https_sg.security_group_id,
        module.test_sg.security_group_id
    ]         
}

# ターゲットグループ(Green)
resource "aws_lb_target_group" "green" {
  name = "${var.resource_prefix}-${var.service_name}-green-tg"
  target_type = "ip"
  vpc_id = var.vpc_id

  port = 80
  protocol = "HTTP"

  health_check {
    path = "/"
    healthy_threshold = 5
    unhealthy_threshold = 2
    timeout = 5
    interval = 30
    matcher = 200
    port = "traffic-port"
    protocol = "HTTP"
  }

  //依存関係の指定忘れるとたまにdestoryできないことがあるらしいZE!
  depends_on = [
    aws_lb.this
  ]
}

# ターゲットグループ(Blue)
resource "aws_lb_target_group" "blue" {
  name = "${var.resource_prefix}-${var.service_name}-blue-tg"
  target_type = "ip"
  vpc_id = var.vpc_id

  port = 80
  protocol = "HTTP"

  health_check {
    path = "/"
    healthy_threshold = 5
    unhealthy_threshold = 2
    timeout = 5
    interval = 30
    matcher = 200
    port = "traffic-port"
    protocol = "HTTP"
  }

  depends_on = [
    aws_lb.this
  ]
}

resource "aws_lb_listener" "prod" {
    load_balancer_arn = aws_lb.this.arn
    port = "80"
    protocol = "HTTP"

    default_action {
      type = "forward"
      target_group_arn = aws_lb_target_group.blue.arn
    }
}

resource "aws_lb_listener" "test" {
    load_balancer_arn = aws_lb.this.arn
    port = "8080"
    protocol = "HTTP"

    default_action {
      type = "forward"
      target_group_arn = aws_lb_target_group.green.arn
    }
}