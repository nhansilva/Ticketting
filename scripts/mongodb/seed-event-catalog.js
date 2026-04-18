// ============================================================
// Seed script: Event Catalog Service — MongoDB
// Run: docker exec -i ticketing-mongodb mongosh ticketing_catalog < seed-event-catalog.js
// Or:  mongosh mongodb://localhost:27017/ticketing_catalog seed-event-catalog.js
// ============================================================

const db = connect("mongodb://localhost:27017/ticketing_catalog");

// ============================================================
// RESET (chỉ chạy khi cần fresh start)
// ============================================================
db.venues.deleteMany({});
db.events.deleteMany({});
db.seats.deleteMany({});

print("🗑️  Cleared venues, events, seats");

// ============================================================
// 1. VENUES
// ============================================================

const venue1Id = "venue-nha-hat-lon-hn";
const venue2Id = "venue-svd-my-dinh";
const venue3Id = "venue-cgv-vincom-ba-trieu";
const venue4Id = "venue-cgv-aeon-tan-phu";

db.venues.insertMany([
  {
    _id: venue1Id,
    name: "Nhà hát Lớn Hà Nội",
    provinceCode: "HN",
    provinceName: "Hà Nội",
    districtCode: "HN-HK",
    districtName: "Hoàn Kiếm",
    streetAddress: "1 Tràng Tiền, Hoàn Kiếm",
    lat: 21.024507,
    lng: 105.857969,
    totalCapacity: 600,
    zones: [
      { name: "VIP",  rows: 5,  seatsPerRow: 20, price: NumberDecimal("1500000"), rowPrefix: "A" },
      { name: "GOLD", rows: 8,  seatsPerRow: 25, price: NumberDecimal("800000"),  rowPrefix: "F" },
      { name: "GA",   rows: 10, rows: 10, seatsPerRow: 30, price: NumberDecimal("350000"), rowPrefix: "N" }
    ],
    createdAt: new Date("2026-01-01T00:00:00Z")
  },
  {
    _id: venue2Id,
    name: "Sân vận động Quốc gia Mỹ Đình",
    provinceCode: "HN",
    provinceName: "Hà Nội",
    districtCode: "HN-NTL",
    districtName: "Nam Từ Liêm",
    streetAddress: "Đường Lê Đức Thọ, Nam Từ Liêm",
    lat: 21.020850,
    lng: 105.763916,
    totalCapacity: 40000,
    zones: [
      { name: "VIP",      rows: 10, seatsPerRow: 30, price: NumberDecimal("3000000"), rowPrefix: "A" },
      { name: "GOLD",     rows: 20, seatsPerRow: 50, price: NumberDecimal("1500000"), rowPrefix: "K" },
      { name: "SILVER",   rows: 15, seatsPerRow: 60, price: NumberDecimal("800000"),  rowPrefix: "E" },
      { name: "STANDING", rows: 5,  seatsPerRow: 100, price: NumberDecimal("500000"), rowPrefix: "Z" }
    ],
    createdAt: new Date("2026-01-01T00:00:00Z")
  },
  {
    _id: venue3Id,
    name: "CGV Vincom Bà Triệu",
    provinceCode: "HN",
    provinceName: "Hà Nội",
    districtCode: "HN-HBT",
    districtName: "Hai Bà Trưng",
    streetAddress: "191 Bà Triệu, Hai Bà Trưng",
    lat: 21.013180,
    lng: 105.845720,
    totalCapacity: 130,
    zones: [
      { name: "STANDARD", rows: 8,  seatsPerRow: 14, price: NumberDecimal("120000"), rowPrefix: "A" },
      { name: "VIP",      rows: 2,  seatsPerRow: 8,  price: NumberDecimal("180000"), rowPrefix: "I" }
    ],
    createdAt: new Date("2026-01-01T00:00:00Z")
  },
  {
    _id: venue4Id,
    name: "CGV Aeon Mall Tân Phú",
    provinceCode: "HCM",
    provinceName: "TP. Hồ Chí Minh",
    districtCode: "HCM-TP",
    districtName: "Tân Phú",
    streetAddress: "30 Bờ Bao Tân Thắng, Sơn Kỳ, Tân Phú",
    lat: 10.800860,
    lng: 106.617630,
    totalCapacity: 150,
    zones: [
      { name: "STANDARD", rows: 9,  seatsPerRow: 14, price: NumberDecimal("120000"), rowPrefix: "A" },
      { name: "VIP",      rows: 2,  seatsPerRow: 9,  price: NumberDecimal("180000"), rowPrefix: "J" }
    ],
    createdAt: new Date("2026-01-01T00:00:00Z")
  }
]);

print("✅ Inserted 4 venues");

// ============================================================
// 2. EVENTS
// ============================================================

const event1Id = "event-son-tung-live-2026";
const event2Id = "event-hieuthuhai-greyd";
const event3Id = "event-coldplay-draft";
const event4Id = "event-avengers-secret-wars-hn";
const event5Id = "event-avengers-secret-wars-hcm";
const event6Id = "event-lanh-lam-anh-oi-hn";

db.events.insertMany([
  // ── CONCERT 1: Sơn Tùng MTP — ON_SALE ───────────────────
  {
    _id: event1Id,
    title: "Sơn Tùng M-TP: SKY TOUR 2026",
    description: "Đêm nhạc đặc biệt của Sơn Tùng M-TP với những bản hit đình đám nhất trong sự nghiệp. Lần đầu tiên SKY TOUR trở lại Hà Nội sau 5 năm.",
    type: "CONCERT",
    status: "ON_SALE",
    venueId: venue1Id,
    venueName: "Nhà hát Lớn Hà Nội",
    startTime: new Date("2026-05-25T19:00:00"),
    endTime:   new Date("2026-05-25T22:00:00"),
    zones: [
      { name: "VIP",  rows: 5,  seatsPerRow: 20, price: NumberDecimal("1500000"), rowPrefix: "A" },
      { name: "GOLD", rows: 8,  seatsPerRow: 25, price: NumberDecimal("800000"),  rowPrefix: "F" },
      { name: "GA",   rows: 10, seatsPerRow: 30, price: NumberDecimal("350000"),  rowPrefix: "N" }
    ],
    imageUrls: [
      "https://cdn.ticketing.com/events/" + event1Id + "/thumbnail.webp",
      "https://cdn.ticketing.com/events/" + event1Id + "/card.webp",
      "https://cdn.ticketing.com/events/" + event1Id + "/banner.webp"
    ],
    detail: {
      type: "CONCERT",
      artists: ["Sơn Tùng M-TP"],
      genres: ["V-Pop", "R&B", "Hip-hop"],
      ageRestriction: 15
    },
    createdBy: "admin",
    createdAt: new Date("2026-04-01T08:00:00Z"),
    updatedAt: new Date("2026-04-10T10:00:00Z")
  },

  // ── CONCERT 2: HIEUTHUHAI x GREY D — ON_SALE ─────────────
  {
    _id: event2Id,
    title: "HIEUTHUHAI x GREY D: TRONG TỪNG HƠI THỞ LIVE CONCERT",
    description: "Đêm nhạc kết hợp giữa hai nghệ sĩ đình đám HIEUTHUHAI và GREY D với setlist dài nhất từ trước đến nay. Sân vận động Mỹ Đình sẽ rung chuyển!",
    type: "CONCERT",
    status: "ON_SALE",
    venueId: venue2Id,
    venueName: "Sân vận động Quốc gia Mỹ Đình",
    startTime: new Date("2026-06-01T18:30:00"),
    endTime:   new Date("2026-06-01T22:30:00"),
    zones: [
      { name: "VIP",      rows: 10, seatsPerRow: 30,  price: NumberDecimal("3000000"), rowPrefix: "A" },
      { name: "GOLD",     rows: 20, seatsPerRow: 50,  price: NumberDecimal("1500000"), rowPrefix: "K" },
      { name: "SILVER",   rows: 15, seatsPerRow: 60,  price: NumberDecimal("800000"),  rowPrefix: "E" },
      { name: "STANDING", rows: 5,  seatsPerRow: 100, price: NumberDecimal("500000"),  rowPrefix: "Z" }
    ],
    imageUrls: [
      "https://cdn.ticketing.com/events/" + event2Id + "/thumbnail.webp",
      "https://cdn.ticketing.com/events/" + event2Id + "/card.webp",
      "https://cdn.ticketing.com/events/" + event2Id + "/banner.webp"
    ],
    detail: {
      type: "CONCERT",
      artists: ["HIEUTHUHAI", "GREY D"],
      genres: ["Hip-hop", "R&B", "Pop"],
      ageRestriction: 16
    },
    createdBy: "admin",
    createdAt: new Date("2026-04-05T08:00:00Z"),
    updatedAt: new Date("2026-04-12T09:00:00Z")
  },

  // ── CONCERT 3: Coldplay — DRAFT (chưa mở bán) ────────────
  {
    _id: event3Id,
    title: "Coldplay: Music Of The Spheres World Tour — Hà Nội",
    description: "Coldplay lần đầu tiên biểu diễn tại Việt Nam! Đêm nhạc hứa hẹn sẽ là sự kiện âm nhạc lớn nhất năm 2026 với hệ thống ánh sáng và màn hình LED đỉnh cao.",
    type: "CONCERT",
    status: "DRAFT",
    venueId: venue2Id,
    venueName: "Sân vận động Quốc gia Mỹ Đình",
    startTime: new Date("2026-09-15T19:00:00"),
    endTime:   new Date("2026-09-15T23:00:00"),
    zones: [
      { name: "DIAMOND", rows: 5,  seatsPerRow: 20,  price: NumberDecimal("5000000"), rowPrefix: "A" },
      { name: "VIP",     rows: 10, seatsPerRow: 30,  price: NumberDecimal("3500000"), rowPrefix: "F" },
      { name: "GOLD",    rows: 20, seatsPerRow: 50,  price: NumberDecimal("2000000"), rowPrefix: "P" },
      { name: "GA",      rows: 10, seatsPerRow: 100, price: NumberDecimal("800000"),  rowPrefix: "Z" }
    ],
    imageUrls: [
      "https://cdn.ticketing.com/events/" + event3Id + "/thumbnail.webp",
      "https://cdn.ticketing.com/events/" + event3Id + "/card.webp",
      "https://cdn.ticketing.com/events/" + event3Id + "/banner.webp"
    ],
    detail: {
      type: "CONCERT",
      artists: ["Coldplay"],
      genres: ["Pop Rock", "Alternative Rock", "Art Pop"],
      ageRestriction: 0
    },
    createdBy: "admin",
    createdAt: new Date("2026-04-15T08:00:00Z"),
    updatedAt: new Date("2026-04-15T08:00:00Z")
  },

  // ── MOVIE 1: Avengers Secret Wars — HN, ON_SALE ──────────
  {
    _id: event4Id,
    title: "Avengers: Secret Wars",
    description: "Cuộc chiến cuối cùng của các Avengers chống lại Kang the Conqueror và đội quân đa vũ trụ. Bom tấn Marvel được mong chờ nhất 2026.",
    type: "MOVIE",
    status: "ON_SALE",
    venueId: venue3Id,
    venueName: "CGV Vincom Bà Triệu",
    startTime: new Date("2026-05-20T14:30:00"),
    endTime:   new Date("2026-05-20T17:30:00"),
    zones: [
      { name: "STANDARD", rows: 8, seatsPerRow: 14, price: NumberDecimal("120000"), rowPrefix: "A" },
      { name: "VIP",      rows: 2, seatsPerRow: 8,  price: NumberDecimal("180000"), rowPrefix: "I" }
    ],
    imageUrls: [
      "https://cdn.ticketing.com/events/" + event4Id + "/thumbnail.webp",
      "https://cdn.ticketing.com/events/" + event4Id + "/card.webp",
      "https://cdn.ticketing.com/events/" + event4Id + "/banner.webp"
    ],
    detail: {
      type: "MOVIE",
      director: "Russo Brothers",
      cast: ["Robert Downey Jr.", "Chris Evans", "Scarlett Johansson", "Benedict Cumberbatch"],
      format: "IMAX",
      durationMinutes: 180,
      rating: "PG13"
    },
    createdBy: "admin",
    createdAt: new Date("2026-04-10T08:00:00Z"),
    updatedAt: new Date("2026-04-10T08:00:00Z")
  },

  // ── MOVIE 2: Avengers Secret Wars — HCM, ON_SALE ─────────
  {
    _id: event5Id,
    title: "Avengers: Secret Wars",
    description: "Cuộc chiến cuối cùng của các Avengers chống lại Kang the Conqueror và đội quân đa vũ trụ. Bom tấn Marvel được mong chờ nhất 2026.",
    type: "MOVIE",
    status: "ON_SALE",
    venueId: venue4Id,
    venueName: "CGV Aeon Mall Tân Phú",
    startTime: new Date("2026-05-20T19:00:00"),
    endTime:   new Date("2026-05-20T22:00:00"),
    zones: [
      { name: "STANDARD", rows: 9, seatsPerRow: 14, price: NumberDecimal("120000"), rowPrefix: "A" },
      { name: "VIP",      rows: 2, seatsPerRow: 9,  price: NumberDecimal("180000"), rowPrefix: "J" }
    ],
    imageUrls: [
      "https://cdn.ticketing.com/events/" + event5Id + "/thumbnail.webp",
      "https://cdn.ticketing.com/events/" + event5Id + "/card.webp",
      "https://cdn.ticketing.com/events/" + event5Id + "/banner.webp"
    ],
    detail: {
      type: "MOVIE",
      director: "Russo Brothers",
      cast: ["Robert Downey Jr.", "Chris Evans", "Scarlett Johansson", "Benedict Cumberbatch"],
      format: "3D",
      durationMinutes: 180,
      rating: "PG13"
    },
    createdBy: "admin",
    createdAt: new Date("2026-04-10T08:00:00Z"),
    updatedAt: new Date("2026-04-10T08:00:00Z")
  },

  // ── MOVIE 3: Lạnh Lắm Anh Ơi — HN, ON_SALE ──────────────
  {
    _id: event6Id,
    title: "Lạnh Lắm Anh Ơi",
    description: "Bộ phim tình cảm lãng mạn Việt Nam về câu chuyện tình yêu xuyên thời gian giữa cô gái Hà Nội và chàng trai Sài Gòn.",
    type: "MOVIE",
    status: "ON_SALE",
    venueId: venue3Id,
    venueName: "CGV Vincom Bà Triệu",
    startTime: new Date("2026-05-22T10:00:00"),
    endTime:   new Date("2026-05-22T12:00:00"),
    zones: [
      { name: "STANDARD", rows: 8, seatsPerRow: 14, price: NumberDecimal("120000"), rowPrefix: "A" },
      { name: "VIP",      rows: 2, seatsPerRow: 8,  price: NumberDecimal("150000"), rowPrefix: "I" }
    ],
    imageUrls: [
      "https://cdn.ticketing.com/events/" + event6Id + "/thumbnail.webp",
      "https://cdn.ticketing.com/events/" + event6Id + "/card.webp",
      "https://cdn.ticketing.com/events/" + event6Id + "/banner.webp"
    ],
    detail: {
      type: "MOVIE",
      director: "Trịnh Đình Lê Minh",
      cast: ["Kaity Nguyễn", "Will (365daband)", "Thu Trang"],
      format: "2D",
      durationMinutes: 105,
      rating: "PG"
    },
    createdBy: "admin",
    createdAt: new Date("2026-04-08T08:00:00Z"),
    updatedAt: new Date("2026-04-08T08:00:00Z")
  }
]);

print("✅ Inserted 6 events (3 concerts + 3 movies)");

// ============================================================
// 3. SEATS — auto-generate cho ON_SALE events
//    Logic giống SeatService.generateSeats()
//    rowLabel = rowPrefix[0] + rowIndex  (A→B→C...)
// ============================================================

function generateSeats(eventId, zones) {
  const seats = [];
  for (const zone of zones) {
    for (let r = 0; r < zone.rows; r++) {
      const rowLabel = String.fromCharCode(zone.rowPrefix.charCodeAt(0) + r);
      for (let s = 1; s <= zone.seatsPerRow; s++) {
        seats.push({
          eventId:    eventId,
          zone:       zone.name,
          row:        rowLabel,
          seatNumber: s,
          code:       rowLabel + s,
          price:      zone.price,
          status:     "AVAILABLE"
        });
      }
    }
  }
  return seats;
}

// Seed seats cho các ON_SALE events
const onSaleEvents = db.events.find({ status: "ON_SALE" }).toArray();
let totalSeats = 0;

for (const event of onSaleEvents) {
  const seats = generateSeats(event._id, event.zones);
  // Fake vài ghế đã SOLD / LOCKED để UI realistic
  if (seats.length > 10) {
    seats[0].status = "SOLD";
    seats[1].status = "SOLD";
    seats[2].status = "LOCKED";
    seats[5].status = "SOLD";
    seats[6].status = "LOCKED";
  }
  db.seats.insertMany(seats);
  totalSeats += seats.length;
  print("  → " + event.title + ": " + seats.length + " ghế");
}

print("✅ Inserted " + totalSeats + " seats for " + onSaleEvents.length + " ON_SALE events");

// ============================================================
// 4. VERIFY
// ============================================================
print("\n📊 Summary:");
print("   Venues : " + db.venues.countDocuments());
print("   Events : " + db.events.countDocuments());
print("   Seats  : " + db.seats.countDocuments());
print("   Seats AVAILABLE : " + db.seats.countDocuments({ status: "AVAILABLE" }));
print("   Seats SOLD      : " + db.seats.countDocuments({ status: "SOLD" }));
print("   Seats LOCKED    : " + db.seats.countDocuments({ status: "LOCKED" }));

print("\n🎉 Seed complete!");
