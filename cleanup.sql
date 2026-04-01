USE XeNow;

-- Migration existing driver licenses
INSERT INTO DriverLicense (UserID, LicenseNumber, LicenseClass, IssueDate, ExpiryDate)
SELECT UserID, DriverLicense, DriverLicenseClass, DriverLicenseIssueDate, DriverLicenseExpiry
FROM Customer
WHERE DriverLicense IS NOT NULL AND DriverLicense != ''
ON DUPLICATE KEY UPDATE LicenseNumber = LicenseNumber;

-- Clean Customer 
ALTER TABLE Customer DROP COLUMN DriverLicense;
ALTER TABLE Customer DROP COLUMN DriverLicenseClass;
ALTER TABLE Customer DROP COLUMN DriverLicenseIssueDate;
ALTER TABLE Customer DROP COLUMN DriverLicenseExpiry;

-- Clean Vehicle
ALTER TABLE Vehicle DROP COLUMN DeletedAt;
