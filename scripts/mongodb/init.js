// MongoDB init script — chạy khi container khởi động lần đầu
// Tạo databases và users cho event-catalog-service và cms-service

db = db.getSiblingDB('event_catalog_db');
db.createUser({
    user: 'ticketing',
    pwd: 'ticketing123',
    roles: [{ role: 'readWrite', db: 'event_catalog_db' }]
});
db.createCollection('events');
db.createCollection('venues');
// Index cho event search
db.events.createIndex({ status: 1, startTime: 1 });
db.events.createIndex({ type: 1 });
db.events.createIndex({ venueId: 1 });
print('event_catalog_db initialized');

db = db.getSiblingDB('cms_db');
db.createUser({
    user: 'ticketing',
    pwd: 'ticketing123',
    roles: [{ role: 'readWrite', db: 'cms_db' }]
});
db.createCollection('homepage_configs');
db.createCollection('banners');
db.createCollection('featured_events');
print('cms_db initialized');
