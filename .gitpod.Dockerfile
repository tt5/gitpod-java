FROM gitpod/workspace-full:latest

RUN sudo install-packages \
	tmux

RUN wget https://github.com/civo/cli/releases/download/v1.0.61/civo-1.0.61-linux-amd64.tar.gz && \
    tar -xvf civo-1.0.61-linux-amd64.tar.gz && \
    chmod +x civo && \
    sudo mv ./civo /usr/local/bin/

RUN wget https://github.com/triggermesh/tmctl/releases/download/v1.4.0/tmctl_linux_amd64.tar.gz && \
    tar -xvf tmctl_linux_amd64.tar.gz && \
    chmod +x tmctl && \
    sudo mv ./tmctl /usr/local/bin/

RUN curl -fsSL -o get_helm.sh https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 && \
    chmod 700 get_helm.sh && \
    ./get_helm.sh

RUN curl -LO https://storage.googleapis.com/kubernetes-release/release/v1.27.4/bin/linux/amd64/kubectl && \
    chmod +x ./kubectl && \
    sudo mv ./kubectl /usr/local/bin/kubectl && \
    mkdir ~/.kube
    
RUN echo 'alias k="kubectl"' >> /home/gitpod/.bashrc
