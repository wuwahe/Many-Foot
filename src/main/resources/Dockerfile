# 多阶段构建 - 基础镜像
FROM ubuntu:22.04 AS base

# 避免交互式提示
ENV DEBIAN_FRONTEND=noninteractive
ENV TZ=Asia/Shanghai

# 更新系统并安装基础工具
RUN apt-get update && apt-get upgrade -y && \
    apt-get install -y \
    curl \
    wget \
    git \
    vim \
    nano \
    tree \
    htop \
    build-essential \
    cmake \
    pkg-config \
    net-tools \
    iputils-ping \
    telnet \
    zip \
    unzip \
    tar \
    gzip \
    ca-certificates \
    gnupg \
    lsb-release \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# Python 环境阶段
FROM base AS python-runtime

RUN apt-get update && apt-get install -y \
    python3.11 \
    python3.11-dev \
    python3.11-venv \
    python3-pip \
    && rm -rf /var/lib/apt/lists/*

# 创建 python3 软链接
RUN update-alternatives --install /usr/bin/python python /usr/bin/python3.11 1 && \
    update-alternatives --install /usr/bin/python3 python3 /usr/bin/python3.11 1

# 升级 pip
RUN python -m pip install --upgrade pip setuptools wheel

# 安装常用 Python 库
RUN pip install --no-cache-dir \
    requests \
    httpx \
    beautifulsoup4 \
    lxml \
    pandas \
    numpy \
    openpyxl \
    xlrd \
    scipy \
    matplotlib \
    seaborn \
    scikit-learn \
    pytest \
    pytest-cov \
    black \
    flake8 \
    pylint \
    ipython \
    jupyter \
    python-dotenv \
    pyyaml \
    python-dateutil

# 最终镜像 - 从 python-runtime 阶段构建
FROM python-runtime AS final

# 创建工作目录
WORKDIR /workspace

# 创建必要的目录（使用 root 用户）
RUN mkdir -p /workspace/code \
             /workspace/data \
             /workspace/output \
             /workspace/logs

# 设置环境变量
ENV PATH="/root/.local/bin:$PATH"
ENV PYTHONUNBUFFERED=1
ENV PYTHONDONTWRITEBYTECODE=1
ENV PIP_NO_CACHE_DIR=1

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD python --version || exit 1

# 默认命令
CMD ["/bin/bash"]
