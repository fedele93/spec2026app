#!/usr/bin/env python3
"""Controlla pwa/shared/event-data.json prima di pubblicare.

Uso:  python3 tools/check-event-data.py            # avvisi (esce con 0)
      python3 tools/check-event-data.py --strict   # gli IBAN segnaposto fanno fallire (esce con 1)

Controlli sempre bloccanti: JSON valido, id dei regali unici, laureandi presenti, segnaposto
degli orari che citano solo chiavi di "schedule", auguri rivolti a laureandi esistenti, punti
mappa con coordinate plausibili (zona Bari). Controllo IBAN (resto 97, ISO 7064): avviso, oppure
errore con --strict (usato dal workflow di release, così non si va online con IBAN finti).
"""
import json
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "pwa", "shared", "event-data.json")
SCHEDULE_KEYS = {"ceremonyDate", "ceremonyTime", "partyDate", "partyTime", "partyEndTime", "busDepartureTime", "busReturnTime"}
PLACEHOLDER = re.compile(r"\{(\w+)(?:\|[^}]*)?\}")
IBAN_RE = re.compile(r"^[A-Z]{2}\d{2}[A-Z0-9]{11,30}$")


def iban_is_valid(iban: str) -> bool:
    s = re.sub(r"\s+", "", iban or "").upper()
    if not IBAN_RE.match(s):
        return False
    digits = "".join(str(int(ch, 36)) for ch in s[4:] + s[:4])
    return int(digits) % 97 == 1


def walk_strings(obj):
    if isinstance(obj, str):
        yield obj
    elif isinstance(obj, list):
        for x in obj:
            yield from walk_strings(x)
    elif isinstance(obj, dict):
        for v in obj.values():
            yield from walk_strings(v)


def main() -> int:
    strict = "--strict" in sys.argv
    errors, warnings = [], []
    with open(SRC, encoding="utf-8") as f:
        d = json.load(f)

    graduates = d.get("graduates", [])
    if not graduates:
        errors.append("nessun laureando in 'graduates'")
    if len(set(graduates)) != len(graduates):
        errors.append("laureandi duplicati")

    targets = d.get("giftTargets", [])
    ids = [t.get("id") for t in targets]
    if len(set(ids)) != len(ids):
        errors.append("id dei regali duplicati: %s" % sorted({i for i in ids if ids.count(i) > 1}))
    if len(targets) != len(graduates):
        warnings.append("%d regali per %d laureandi" % (len(targets), len(graduates)))
    for t in targets:
        for legacy in ("targetAmount", "collectedAmount"):
            if legacy in t:
                errors.append("regalo '%s': il campo %s non esiste più (nessuna cifra viene mostrata)" % (t.get("id"), legacy))
    if "giftContributions" in d:
        errors.append("'giftContributions' non esiste più: le quote di esempio vanno in 'giftPoolContributions'")

    collector = d.get("giftCollector") or {}
    if not collector.get("name"):
        errors.append("manca 'giftCollector' (cassiere delle quote uniche) o il suo nome")
    bad_methods = [m for m in collector.get("paymentMethods", []) if m not in ("IBAN", "PayPal", "Contanti")]
    if bad_methods:
        errors.append("giftCollector.paymentMethods ammette solo IBAN, PayPal, Contanti: %s" % bad_methods)
    ids_set = set(ids)
    avatars = (d.get("assistant") or {}).get("avatars", [])
    for a in avatars:
        if a.get("id") not in ids_set:
            errors.append("avatar dell'assistente con id sconosciuto (deve essere uno dei regali): %s" % a.get("id"))
    if avatars and not any(a.get("enabled") for a in avatars):
        warnings.append("nessun avatar dell'assistente abilitato in assistant.avatars")
    for c in d.get("giftPoolContributions", []):
        for a in c.get("allocations", []):
            if a.get("graduateId") not in ids_set:
                errors.append("quota di '%s' ripartita su un regalo sconosciuto: %s" % (c.get("donorName"), a.get("graduateId")))

    for k in (d.get("schedule") or {}):
        if k not in SCHEDULE_KEYS:
            errors.append("chiave sconosciuta in schedule: %s" % k)
    for s in walk_strings({k: v for k, v in d.items() if k in ("program", "busSchedule", "mapPoints")}):
        for key in PLACEHOLDER.findall(s):
            if key not in SCHEDULE_KEYS:
                errors.append("segnaposto {%s} non corrisponde a nessuna chiave di schedule" % key)

    allowed_targets = set(graduates) | {"Tutti i Laureandi"}
    for w in d.get("wishes", []):
        if w.get("targetGraduate") not in allowed_targets:
            errors.append("augurio di '%s' rivolto a un destinatario sconosciuto: %s" % (w.get("authorName"), w.get("targetGraduate")))

    for p in d.get("mapPoints", []):
        lat, lon = float(p.get("latitude", 0)), float(p.get("longitude", 0))
        if not (40.9 <= lat <= 41.3 and 16.6 <= lon <= 17.1):
            errors.append("punto mappa '%s' fuori dalla zona di Bari: %s, %s" % (p.get("id"), lat, lon))

    bad_ibans = [t.get("id") for t in targets if not iban_is_valid(t.get("iban", ""))]
    if collector and not iban_is_valid(collector.get("iban", "")):
        bad_ibans.append("giftCollector")
    if bad_ibans:
        msg = "IBAN non validi (segnaposto?) nei regali: %s" % ", ".join(bad_ibans)
        (errors if strict else warnings).append(msg)

    for w in warnings:
        print("::warning::" if os.environ.get("GITHUB_ACTIONS") else "AVVISO:", w)
    for e in errors:
        print("::error::" if os.environ.get("GITHUB_ACTIONS") else "ERRORE:", e)
    if errors:
        return 1
    print("event-data.json ok (%d laureandi, %d regali, %d avvisi)" % (len(graduates), len(targets), len(warnings)))
    return 0


if __name__ == "__main__":
    sys.exit(main())
