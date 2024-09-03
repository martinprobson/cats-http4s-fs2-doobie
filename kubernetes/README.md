# Kubernetes Setup - Notes

## Microk8s Setup

[microk8s](https://microk8s.io/)

### Install microk8s 
Install microk8s on ubuntu using snap: -

```bash
snap install microk8s --classic
```

### Start up microk8s

```bash
microk8s start
```

### microk8s status

```bash
microk8s status
```

### Dashboard
Enable the dashboard service with: -

```bash
microk8s enable dashboard
```

### Use dashboard proxy for access
```bash
microk8s dashboard-proxy
```

Dashboard available at `https://127.0.0.1:10443`

### Enable microk8s built-in registry

```bash
microk8s enable registry
```

### Enable loadbalencer

```bash
microk8s enable metallb
```

### Build and tag the docker image 
Build and tag the docker image, so that it can be successfully pushed to the built-in registry: -

cd to project root directory (with a `Dockerfile`)

```bash
docker build . -t localhost:32000/server:latest
docker push localhost:32000/server:latest
```

### Apply the k8s configuration to the cluster

Goto the kubernetes directory

```bash
kubectl apply -k ./
```
