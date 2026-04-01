-- Xóa tất cả các bản ghi có khóa ngoại tới Booking trước
DELETE FROM Payment;
DELETE FROM Contract;
DELETE FROM Invoice;

-- Cuối cùng xóa các bản ghi trong bảng Booking
DELETE FROM Booking;
