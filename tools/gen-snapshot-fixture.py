#!/usr/bin/env python3
"""Rigenera app/src/test/resources/snapshot.json dal backend (repo neuroparty-backend).

Il file è una risposta reale di GET /api/snapshot con i dati demo più qualche record inserito
via API (prenotazione, augurio, foto caricata, quota, notifica), usata dai test Android per
verificare che i modelli Kotlin leggano davvero ciò che il server produce.

Uso:  python3 tools/gen-snapshot-fixture.py [--backend ../neuroparty-backend]   # riscrive il fixture
      python3 tools/gen-snapshot-fixture.py --check                             # esce con 1 se il fixture è vecchio
Richiede le dipendenze del backend (pip install -r requirements-dev.txt del backend).
"""
import argparse
import io
import json
import os
import sys
import tempfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DST = os.path.join(ROOT, "app", "src", "test", "resources", "snapshot.json")


def build_snapshot(backend_dir: str) -> dict:
    tmp = tempfile.mkdtemp(prefix="np-fixture-")
    # app/main.py del backend crea anche un'app "di modulo" (per uvicorn) con le impostazioni
    # di default: le dirottiamo in una cartella temporanea per non creare data/ in questo repo.
    os.environ["DATA_DIR"] = os.path.join(tmp, "import-data")
    os.environ["SEED_FILE"] = os.path.join(ROOT, "pwa", "shared", "event-data.json")
    sys.path.insert(0, backend_dir)
    from fastapi.testclient import TestClient  # noqa: E402
    from PIL import Image  # noqa: E402

    from app.config import Settings  # noqa: E402
    from app.main import create_app  # noqa: E402

    settings = Settings(
        data_dir=os.path.join(tmp, "data"),
        seed_file=os.path.join(ROOT, "pwa", "shared", "event-data.json"),  # sempre il JSON di questo repo
        seed_demo_data=True,
        admin_token="fixture-token",
        public_url="https://festa.example.org",
    )
    admin = {"X-Admin-Token": "fixture-token"}
    dev = {"X-Client-Id": "dev-fixture"}
    with TestClient(create_app(settings)) as c:
        c.post("/api/bus/bookings", json={"passengerName": "Famiglia Regina", "seatsCount": 3, "pickupStop": "Stazione Ferroviaria Centrale di Bari", "returnTripWanted": True, "contactPhone": "347 0001122", "notes": "Arriviamo in treno"}, headers=dev)
        c.post("/api/wishes", json={"authorName": "Un amico", "message": "Auguri a tutti e nove! Ci vediamo al Giardino dei Tempi.", "targetGraduate": "Tutti i Laureandi", "emojiBadge": "🥂"}, headers=dev)
        img = Image.new("RGB", (800, 600), (30, 60, 120))
        buf = io.BytesIO()
        img.save(buf, "JPEG")
        c.post("/api/photos", files={"file": ("festa.jpg", buf.getvalue(), "image/jpeg")}, data={"authorName": "Staff Organizzazione", "caption": "Sopralluogo al Giardino dei Tempi: la sala è pronta!"}, headers=dev)
        c.post("/api/gifts/contributions", json={"donorName": "Amici di Donato", "targetGraduateId": "regina", "amount": 80, "paymentMethod": "Satispay", "note": "Per il nuovo ecografo!", "isAnonymous": False}, headers=dev)
        c.post("/api/notifications", json={"title": "📍 Festa al Giardino dei Tempi", "message": "Confermata la sede della festa di venerdì 13 novembre: Il Giardino dei Tempi - Orto Botanico, Via Giovanni Amendola 247, Bari.", "category": "Festa"}, headers=admin)
        return c.get("/api/snapshot").json()


def signature(snap: dict) -> dict:
    """Parte 'stabile' dello snapshot: cambia solo se cambiano dati o formato, non ad ogni esecuzione."""
    return {
        "keys": sorted(snap.keys()),
        "event": snap["event"],
        "counts": {k: len(v) for k, v in snap.items() if isinstance(v, list)},
        "giftTargets": [t["id"] for t in snap["giftTargets"]],
        "recordKeys": {k: sorted(v[0].keys()) for k, v in snap.items() if isinstance(v, list) and v},
    }


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--backend", default=os.path.join(ROOT, "..", "neuroparty-backend"), help="clone del repo neuroparty-backend")
    ap.add_argument("--check", action="store_true", help="non scrive: esce con 1 se il fixture non corrisponde")
    args = ap.parse_args()
    backend = os.path.abspath(args.backend)
    if not os.path.isdir(os.path.join(backend, "app")):
        print("Backend non trovato in %s (usa --backend)" % backend)
        return 2

    snap = build_snapshot(backend)
    if args.check:
        with open(DST, encoding="utf-8") as f:
            current = json.load(f)
        if signature(current) != signature(snap):
            print("::error::" if os.environ.get("GITHUB_ACTIONS") else "ERRORE:",
                  "snapshot.json non è allineato al backend/JSON: esegui 'python3 tools/gen-snapshot-fixture.py' e committa.")
            return 1
        print("snapshot.json allineato al backend")
        return 0

    with open(DST, "w", encoding="utf-8") as f:
        json.dump(snap, f, indent=4, ensure_ascii=True)
        f.write("\n")
    print("Scritto %s (%s)" % (os.path.relpath(DST, ROOT), ", ".join("%s=%d" % kv for kv in signature(snap)["counts"].items())))
    return 0


if __name__ == "__main__":
    sys.exit(main())
