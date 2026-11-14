"""Turn deepstash-content.pdf into the app's content assets.

Usage (from repo root, needs poppler's `pdftotext`):
    python3 tools/pdf_to_assets.py deepstash-content.pdf

Writes app/src/main/assets/{cats,books,ideas}.tsv. Idea id in the app = bookIndex * 10 + ideaIndex.
"""
import os
import re
import subprocess
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(ROOT, "app/src/main/assets")

CATS = [
    ("Productivity", "#F5B841"), ("Money", "#3DDC97"), ("Psychology", "#8C7CF0"),
    ("Thinking", "#4FC3F7"), ("Self-Improvement", "#FF8A5B"), ("Communication", "#29B6F6"),
    ("Business", "#5B8DEF"), ("Philosophy", "#E0A96D"), ("Happiness", "#FFD54F"),
    ("Mindfulness", "#9CCC65"), ("Health", "#66BB6A"), ("Learning", "#FF6FA5"),
    ("Science", "#26C6DA"), ("Relationships", "#F06292"),
]
# 1-indexed book number (order in the PDF) -> topic
GROUPS = {
    "Productivity": [2, 5, 31, 32, 33, 34, 118, 119],
    "Money": [3, 11, 14, 41, 42, 43, 44, 45, 46, 47, 48],
    "Psychology": [6, 13, 20, 27, 38, 52, 53, 54, 56, 57, 58, 71, 72, 73],
    "Thinking": [4, 50, 51, 55, 88, 89, 90, 91, 92, 93],
    "Self-Improvement": [1, 9, 10, 21, 26, 115],
    "Communication": [7, 37, 39, 40, 113, 114],
    "Business": [17, 25, 28, 29, 30, 36, 49, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 117],
    "Philosophy": [8, 15, 19, 22, 23, 24, 101, 102, 103, 104, 105, 107, 108, 109, 110],
    "Happiness": [74, 75, 76, 77, 78, 79, 80, 106],
    "Mindfulness": [12, 18, 116],
    "Health": [81, 82],
    "Learning": [35, 83, 84, 85, 86, 87, 120],
    "Science": [16, 94, 95, 96, 97, 98, 99, 100],
    "Relationships": [111, 112],
}

SKIP_LINE = re.compile(r"^(Page \d+ of \d+|100 Idea Cards from 10 Great Books|Part \d+:.*)$")
SKIP_PARA = re.compile(r"^(Bite-sized|These cards are|As before|That completes|One hundred more|Ten more books|Batch \d+)")
HEAD = re.compile(r"^(\d{1,3})\. (.+?)(?: by (.+))?$")


def parse(text):
    books, paras, cur = [], [], []
    wrap = False

    def flush():
        if cur:
            paras.append(" ".join(cur))
            cur.clear()

    # 1. paragraphs (blank-line separated), dropping page headers/footers
    for raw in text.split("\n"):
        s = raw.strip().replace("\f", "")
        if SKIP_LINE.match(s):
            continue
        if not s:
            flush()
            continue
        m = HEAD.match(s)
        if m and int(m.group(1)) == len(books) + 1:
            flush()
            title, author = m.group(2).strip(), (m.group(3) or "").strip()
            bm = re.match(r"^(.*?) \((.+)\)$", title)
            if not author and bm:  # e.g. "The Bhagavad Gita (Teachings on ...)"
                title, author = bm.group(1), "Traditional"
            books.append({"t": title, "a": author, "ideas": []})
            paras.append(None)  # marker: new book
            wrap = author.endswith((" and", ","))
            continue
        if books and wrap:  # author wrapped onto the next line
            wrap = False
            name, _, s = s.partition(" ")
            books[-1]["a"] += " " + name
            if not s:
                continue
        cur.append(s)
    flush()

    # 2. paragraphs -> ideas; a page break can split a paragraph (continuation starts lowercase)
    bi = -1
    for p in paras:
        if p is None:
            bi += 1
            continue
        if bi < 0 or SKIP_PARA.match(p):
            continue
        ideas = books[bi]["ideas"]
        if ideas and (p[0].islower() or not re.search(r"[.!?\"')]$", ideas[-1])):
            ideas[-1] += " " + p
        else:
            ideas.append(p)

    # authors wrapped mid-name: the surname leaked into the first idea
    for b in books:
        if b["ideas"] and (b["a"].endswith(("William", "Fumitake", "Mark", "Anna Rosling")) or b["a"].endswith(" and")):
            name, _, rest = b["ideas"][0].partition(" ")
            b["a"] += " " + name
            b["ideas"][0] = rest

    # "Headline. Body text." -> (headline, body)
    for b in books:
        out = []
        for t in b["ideas"]:
            m = re.match(r"^(.+?[.!?])\s+(.+)$", t)
            out.append((m.group(1).rstrip("."), m.group(2)) if m else (t.rstrip("."), ""))
        b["ideas"] = out
    return books


def main():
    pdf = sys.argv[1] if len(sys.argv) > 1 else os.path.join(ROOT, "deepstash-content.pdf")
    text = subprocess.check_output(["pdftotext", "-layout", pdf, "-"]).decode("utf-8")
    books = parse(text)

    cat_idx = {n: i for i, (n, _) in enumerate(CATS)}
    cat_of = {n: cat_idx[name] for name, nums in GROUPS.items() for n in nums}
    missing = set(range(1, len(books) + 1)) - set(cat_of)
    if missing:
        sys.exit(f"books without a topic in GROUPS: {sorted(missing)}")

    clean = lambda s: str(s).replace("\t", " ").replace("\n", " ").strip()
    def tsv(name, rows):
        with open(os.path.join(ASSETS, name), "w", encoding="utf-8") as f:
            for r in rows:
                f.write("\t".join(clean(x) for x in r) + "\n")

    rows = [(b["t"], b["a"], cat_of[i + 1]) for i, b in enumerate(books)]
    # extra hand-written books are appended after the PDF ones so existing idea ids never shift
    with open(os.path.join(ROOT, "tools/extra_books.tsv"), encoding="utf-8") as f:
        for line in filter(None, (l.rstrip("\n") for l in f)):
            if line.startswith("#"):
                t, a, c = line[1:].split("\t")
                rows.append((t, a, int(c)))
                books.append({"t": t, "a": a, "ideas": []})
            else:
                books[-1]["ideas"].append(tuple(line.split("\t", 1)))

    os.makedirs(ASSETS, exist_ok=True)
    tsv("cats.tsv", CATS)
    tsv("books.tsv", rows)
    tsv("ideas.tsv", [idea for b in books for idea in b["ideas"]])
    odd = [(i + 1, b["t"], len(b["ideas"])) for i, b in enumerate(books) if len(b["ideas"]) != 10]
    print(f"{len(books)} books, {sum(len(b['ideas']) for b in books)} ideas -> {ASSETS}" + (f"; not 10 ideas: {odd}" if odd else ""))


if __name__ == "__main__":
    main()
