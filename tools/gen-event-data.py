#!/usr/bin/env python3
"""Codegen: trasforma pwa/shared/event-data.json in SeedData.kt.

Singola sorgente dati: modificare il JSON per aggiornare PWA e Android insieme.
Lancia:  python3 tools/gen-event-data.py
Genera:  app/src/main/java/com/example/data/SeedData.kt
"""
import json
import os
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "pwa", "shared", "event-data.json")
DST = os.path.join(ROOT, "app", "src", "main", "java", "com", "example", "data", "SeedData.kt")

with open(SRC, encoding="utf-8") as f:
    d = json.load(f)

# Distanze relative (ore) -> offset in millisecondi al primo avvio.
def age_ms(age_hours):
    return f"(System.currentTimeMillis() - {int(round(float(age_hours or 0) * 3600000))}L)"

def kt_str(s):
    s = "" if s is None else str(s)
    return '"' + s.replace("\\", "\\\\").replace('"', '\\"') + '"'

def kt_bool(b):
    return "true" if b else "false"

out = []
out.append("package com.example.data")
out.append("")
out.append("import com.example.ui.MapPoint")
out.append("")
out.append("// FILE GENERATO DA tools/gen-event-data.py a partire da pwa/shared/event-data.json.")
out.append("// NON modificare a mano: cambia il JSON e rigenera con: python3 tools/gen-event-data.py")
out.append("object SeedData {")
out.append("    const val maxBusSeats: Int = %d" % int(d.get("meta", {}).get("maxBusSeats", 54)))
out.append("")
out.append("    val graduates: List<String> = listOf(")
out.append("".join("        %s,\n" % kt_str(g) for g in d.get("graduates", [])))
out.append("    )")
out.append("")

# Map points
out.append("    val mapPoints: List<MapPoint> = listOf(")
for p in d.get("mapPoints", []):
    out.append("        MapPoint(")
    out.append("            id = %s," % kt_str(p.get("id")))
    out.append("            title = %s," % kt_str(p.get("title")))
    out.append("            subtitle = %s," % kt_str(p.get("subtitle")))
    out.append("            timeLabel = %s," % kt_str(p.get("timeLabel")))
    out.append("            address = %s," % kt_str(p.get("address")))
    out.append("            latitude = %s," % repr(float(p.get("latitude", 0.0))))
    out.append("            longitude = %s," % repr(float(p.get("longitude", 0.0))))
    out.append("            iconType = %s," % kt_str(p.get("iconType")))
    out.append("            description = %s" % kt_str(p.get("description")))
    out.append("        ),")
out.append("    )")
out.append("")

# Program info (dati testuali; le icone/colori Compose restano nello screen)
prog = d.get("program", {})
out.append("    val programBadge: String = %s" % kt_str(prog.get("badge")))
out.append("    val programTitle: String = %s" % kt_str(prog.get("title")))
out.append("    val programSubtitle: String = %s" % kt_str(prog.get("subtitle")))
out.append("    val programDateLabel: String = %s" % kt_str(prog.get("dateLabel")))
out.append("    val programLocationLabel: String = %s" % kt_str(prog.get("locationLabel")))
out.append("")
out.append("    val programTimeline: List<ProgramTimelineEntry> = listOf(")
for t in prog.get("timeline", []):
    out.append("        ProgramTimelineEntry(")
    out.append("            time = %s," % kt_str(t.get("time")))
    out.append("            title = %s," % kt_str(t.get("title")))
    out.append("            location = %s," % kt_str(t.get("location")))
    out.append("            details = %s," % kt_str(t.get("details")))
    out.append("            hasMore = %s" % kt_bool(t.get("more", True)))
    out.append("        ),")
out.append("    )")
out.append("")

# Bus schedule (dati testuali)
bus = d.get("busSchedule", {})
out.append("    val busScheduleSubtitle: String = %s" % kt_str(bus.get("subtitle")))
andata = bus.get("andata", {}) or {}
ritorno = bus.get("ritorno", {}) or {}
out.append("    val busAndata: BusTripInfo = BusTripInfo(")
out.append("        timeLabel = %s, from = %s, to = %s, notes = %s" % (
    kt_str(andata.get("timeLabel")), kt_str(andata.get("from")), kt_str(andata.get("to")), kt_str(andata.get("notes"))))
out.append("    )")
out.append("    val busRitorno: BusTripInfo = BusTripInfo(")
out.append("        timeLabel = %s, from = %s, to = %s, notes = %s" % (
    kt_str(ritorno.get("timeLabel")), kt_str(ritorno.get("from")), kt_str(ritorno.get("to")), kt_str(ritorno.get("notes"))))
out.append("    )")
out.append("    val busPickupStops: List<String> = listOf(")
out.append("".join("        %s,\n" % kt_str(s) for s in bus.get("pickupStops", [])))
out.append("    )")
out.append("")

# Guests
out.append("    val guests: List<GuestEntity> = listOf(")
for g in d.get("guests", []):
    out.append("        GuestEntity(")
    out.append("            fullName = %s," % kt_str(g.get("fullName")))
    out.append("            category = %s," % kt_str(g.get("category")))
    out.append("            rsvpStatus = RsvpStatus.%s," % g.get("rsvpStatus"))
    out.append("            guestsCount = %d," % int(g.get("guestsCount", 1)))
    out.append("            dietaryNotes = %s," % kt_str(g.get("dietaryNotes")))
    out.append("            contactInfo = %s" % kt_str(g.get("contactInfo")))
    out.append("        ),")
out.append("    )")
out.append("")

# Bus bookings
out.append("    val busBookings: List<BusBookingEntity> = listOf(")
for b in d.get("busBookings", []):
    out.append("        BusBookingEntity(")
    out.append("            passengerName = %s," % kt_str(b.get("passengerName")))
    out.append("            seatsCount = %d," % int(b.get("seatsCount", 1)))
    out.append("            pickupStop = %s," % kt_str(b.get("pickupStop")))
    out.append("            returnTripWanted = %s," % kt_bool(b.get("returnTripWanted", True)))
    out.append("            contactPhone = %s," % kt_str(b.get("contactPhone")))
    out.append("            notes = %s" % kt_str(b.get("notes")))
    out.append("        ),")
out.append("    )")
out.append("")

# Wishes
out.append("    val wishes: List<WishEntity> = listOf(")
for w in d.get("wishes", []):
    out.append("        WishEntity(")
    out.append("            authorName = %s," % kt_str(w.get("authorName")))
    out.append("            targetGraduate = %s," % kt_str(w.get("targetGraduate")))
    out.append("            message = %s," % kt_str(w.get("message")))
    out.append("            emojiBadge = %s," % kt_str(w.get("emojiBadge")))
    out.append("            heartCount = %d," % int(w.get("heartCount", 0)))
    out.append("            createdAt = %s" % age_ms(w.get("ageHours")))
    out.append("        ),")
out.append("    )")
out.append("")

# Photos
out.append("    val photos: List<SharedPhotoEntity> = listOf(")
for p in d.get("photos", []):
    out.append("        SharedPhotoEntity(")
    out.append("            authorName = %s," % kt_str(p.get("authorName")))
    out.append("            caption = %s," % kt_str(p.get("caption")))
    out.append("            imageResId = %d," % int(p.get("imageResId", 0)))
    out.append("            imageUri = %s," % kt_str(p.get("imageUri", "")))
    out.append("            likesCount = %d," % int(p.get("likesCount", 0)))
    out.append("            createdAt = %s" % age_ms(p.get("ageHours")))
    out.append("        ),")
out.append("    )")
out.append("")

# Gift targets
out.append("    val giftTargets: List<GiftTargetEntity> = listOf(")
for t in d.get("giftTargets", []):
    out.append("        GiftTargetEntity(")
    out.append("            id = %s," % kt_str(t.get("id")))
    out.append("            name = %s," % kt_str(t.get("name")))
    out.append("            specialization = %s," % kt_str(t.get("specialization")))
    out.append("            roleTitle = %s," % kt_str(t.get("roleTitle")))
    out.append("            giftTitle = %s," % kt_str(t.get("giftTitle")))
    out.append("            giftDescription = %s," % kt_str(t.get("giftDescription")))
    out.append("            targetAmount = %s," % format(float(t.get("targetAmount", 0)), ".1f"))
    out.append("            collectedAmount = %s," % format(float(t.get("collectedAmount", 0)), ".1f"))
    out.append("            iban = %s," % kt_str(t.get("iban")))
    out.append("            ibanHolder = %s," % kt_str(t.get("ibanHolder")))
    out.append("            satispayUrl = %s," % kt_str(t.get("satispayUrl")))
    out.append("            paypalMeUrl = %s" % kt_str(t.get("paypalMeUrl")))
    out.append("        ),")
out.append("    )")
out.append("")

# Gift contributions
out.append("    val giftContributions: List<GiftContributionEntity> = listOf(")
for c in d.get("giftContributions", []):
    out.append("        GiftContributionEntity(")
    out.append("            donorName = %s," % kt_str(c.get("donorName")))
    out.append("            targetGraduateId = %s," % kt_str(c.get("targetGraduateId")))
    out.append("            targetGraduateName = %s," % kt_str(c.get("targetGraduateName")))
    out.append("            amount = %s," % format(float(c.get("amount", 0)), ".1f"))
    out.append("            paymentMethod = %s," % kt_str(c.get("paymentMethod")))
    out.append("            note = %s," % kt_str(c.get("note")))
    out.append("            isAnonymous = %s," % kt_bool(c.get("isAnonymous", False)))
    out.append("            contributedAt = %s" % age_ms(c.get("ageHours")))
    out.append("        ),")
out.append("    )")
out.append("")

# Notifications
out.append("    val notifications: List<EventNotificationEntity> = listOf(")
for n in d.get("notifications", []):
    out.append("        EventNotificationEntity(")
    out.append("            title = %s," % kt_str(n.get("title")))
    out.append("            message = %s," % kt_str(n.get("message")))
    out.append("            category = %s," % kt_str(n.get("category")))
    out.append("            timestamp = %s," % age_ms(n.get("ageHours")))
    out.append("            isRead = %s" % kt_bool(n.get("isRead", False)))
    out.append("        ),")
out.append("    )")
out.append("}")
out.append("")
out.append("data class ProgramTimelineEntry(")
out.append("    val time: String,")
out.append("    val title: String,")
out.append("    val location: String,")
out.append("    val details: String,")
out.append("    val hasMore: Boolean")
out.append(")")
out.append("")
out.append("data class BusTripInfo(")
out.append("    val timeLabel: String,")
out.append("    val from: String,")
out.append("    val to: String,")
out.append("    val notes: String")
out.append(")")
out.append("")

os.makedirs(os.path.dirname(DST), exist_ok=True)
with open(DST, "w", encoding="utf-8") as f:
    f.write("\n".join(out))
print("Generato: %s" % os.path.relpath(DST, ROOT))
