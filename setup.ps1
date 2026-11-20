# Sobe todo o ambiente no Minikube (rodar na raiz do projeto)
# Uso: powershell -ExecutionPolicy Bypass -File .\setup.ps1

Set-Location $PSScriptRoot

function Passo($texto) {
    Write-Host ""
    Write-Host "==== $texto ====" -ForegroundColor Cyan
}
function Checar {
    if ($LASTEXITCODE -ne 0) {
        Write-Host "ERRO no passo acima. Copie a mensagem de erro e envie." -ForegroundColor Red
        exit 1
    }
}

Passo "1/7 Namespaces (app e monitoring)"
kubectl apply -f k8s/namespaces.yaml; Checar

Passo "2/7 Banco de dados (PostgreSQL)"
kubectl apply -f k8s/banco/; Checar

Passo "3/7 Loki, Tempo e OpenTelemetry Collector"
kubectl apply -f k8s/observabilidade/loki.yaml -f k8s/observabilidade/tempo.yaml -f k8s/observabilidade/otel-collector.yaml; Checar

Passo "4/7 Prometheus + Grafana (Helm: kube-prometheus-stack)"
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update; Checar
helm upgrade --install kps prometheus-community/kube-prometheus-stack -n monitoring -f k8s/observabilidade/helm/values-kube-prometheus-stack.yaml --timeout 15m; Checar
kubectl apply -f k8s/observabilidade/app-servicemonitor.yaml; Checar

Passo "5/7 Build da imagem da aplicacao (demora alguns minutos)"
docker build -t tarefas-app:1.0 ./app; Checar

Passo "6/7 Enviando a imagem para dentro do Minikube"
minikube image load tarefas-app:1.0; Checar

Passo "7/7 Aplicacao (2 replicas + Service NodePort)"
kubectl apply -f k8s/app/; Checar

Passo "Aguardando os pods ficarem prontos"
kubectl rollout status deployment/banco -n app --timeout=300s
kubectl rollout status deployment/app -n app --timeout=600s

Passo "Resultado"
kubectl get pods -A
Write-Host ""
Write-Host "Pronto! Proximos passos estao no README.md (secao 'Como acessar')." -ForegroundColor Green
