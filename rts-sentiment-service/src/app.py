from flask import Flask, render_template
from routes.news import news_bp
from config import mongo_client

app = Flask(__name__)

app.register_blueprint(news_bp, url_prefix="/news")

@app.route("/")
def index():
    db = mongo_client["news_app"]
    collection = db["news"]
    articles = list(collection.find().sort("published_at", -1))
    return render_template("index.html", articles=articles)

if __name__ == "__main__":
    app.run(debug=True)