# Helm chart starter for the API Deployment/Service/Ingress.
# Postgres, Redis, Kafka, and Keycloak are expected as external dependencies.
#
# Example:
#   helm upgrade --install shortener ./deploy/helm/url-shortener \
#     --set image.repository=ghcr.io/example/url-shortener \
#     --set image.tag=1.2.3 \
#     --set env.BASE_URL=https://shortener.example.com
