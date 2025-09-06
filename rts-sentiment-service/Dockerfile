# Base image Python
FROM python:3.10-slim

# Set working directory
WORKDIR /app

# Cài đặt các gói cần thiết để build Scrapy + MongoDB driver
RUN apt-get update && apt-get install -y \
    gcc \
    libxml2-dev \
    libxslt-dev \
    libz-dev \
    && rm -rf /var/lib/apt/lists/*

# Copy requirements và cài đặt
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy toàn bộ source code vào container
COPY . .

# Expose port (Flask mặc định 5000, map sang 8000)
EXPOSE 8087

# Khi container start: chạy crawler trước rồi chạy Flask app
CMD ["sh", "-c", "python src/crawler.py && python src/app.py"]
