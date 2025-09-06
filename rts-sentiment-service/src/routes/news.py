from flask import Blueprint, request, Response
from config import mongo_client
from bson import json_util # Thư viện để xử lý ObjectId của Mongo
import json

news_bp = Blueprint('news', __name__)
db = mongo_client["news_app"]
collection = db["news"]

@news_bp.route("/", methods=["GET"])
def get_news_paginated():
    """
    Endpoint này lấy các bài báo đã được phân trang từ MongoDB.
    """
    try:
        page = int(request.args.get('page', 1))
        limit = int(request.args.get('limit', 12))
        skip = (page - 1) * limit

        articles = list(collection.find().sort("published_at", -1).skip(skip).limit(limit))

        total_articles = collection.count_documents({})
        total_pages = (total_articles + limit - 1) // limit

        response_data = {
            'articles': articles,
            'total_pages': total_pages,
            'current_page': page
        }

        # --- BẮT ĐẦU PHẦN SỬA LỖI ---
        # Sử dụng Response object của Flask và json_util để xử lý BSON types một cách an toàn
        return Response(
            response=json_util.dumps(response_data),
            status=200,
            mimetype='application/json'
        )
        # --- KẾT THÚC PHẦN SỬA LỖI ---

    except Exception as e:
        # Trả về lỗi dưới dạng JSON nếu có sự cố
        error_response = {
            "message": "Lỗi khi truy vấn dữ liệu",
            "error": str(e)
        }
        return Response(
            response=json_util.dumps(error_response),
            status=500,
            mimetype='application/json'
        )

@news_bp.route("/trigger_crawl", methods=["POST"])
def trigger_crawl():
    """
    Endpoint (chỉ dành cho admin) để kích hoạt crawl thủ công.
    Thực tế, bạn nên chạy crawler bằng scheduler.
    """
    # Logic chạy crawler vẫn giữ nguyên nhưng không nên dùng cho người dùng cuối
    import subprocess
    result = subprocess.run(
        ["scrapy", "crawl", "finance_spider"],
        cwd="scrapy_crawlers",
        capture_output=True,
        text=True
    )

    if result.returncode != 0:
        return jsonify({"message": "Crawler gặp lỗi", "error": result.stderr}), 500

    count = collection.count_documents({})
    return jsonify({"message": f"Đã crawl thành công. Tổng số bài viết: {count}."})