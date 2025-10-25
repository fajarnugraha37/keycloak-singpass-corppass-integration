# 📋 CA Certificate Installation Instructions

## Windows
1. Double-click on ca/certs/ca.crt
2. Click "Install Certificate..."
3. Select "Local Machine" (requires admin)
4. Select "Place all certificates in the following store"
5. Browse and select "Trusted Root Certification Authorities"
6. Click "Next" and "Finish"

## macOS
`ash
sudo security add-trusted-cert -d -r trustRoot -k /Library/Keychains/System.keychain ca/certs/ca.crt
`

## Linux (Ubuntu/Debian)
`ash
sudo cp ca/certs/ca.crt /usr/local/share/ca-certificates/custom-ca.crt
sudo update-ca-certificates
`

## Browser-specific (Chrome/Edge)
1. Go to Settings > Privacy and Security > Security
2. Click "Manage certificates"
3. Go to "Trusted Root Certification Authorities" tab
4. Click "Import..." and select ca/certs/ca.crt

## Browser-specific (Firefox)
1. Go to Settings > Privacy & Security
2. Scroll to "Certificates" and click "View Certificates"
3. Go to "Authorities" tab
4. Click "Import..." and select ca/certs/ca.crt
5. Check "Trust this CA to identify websites"
