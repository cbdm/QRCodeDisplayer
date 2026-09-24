# QRCodeDisplayer

The goal of this android app is to make QR codes easier to scan. I had some issues trying to scan a parking pass at a machine and thought of this solution.

The app works like this:

- you open an image that contains a QR code in your phone and "share" that image to this app;
- a white border gets added around the image to make it easier to scan codes that don't have anything around them;
- the image is scanned with [Google's barcode scanning](https://developers.google.com/ml-kit/vision/barcode-scanning) to get the content of the code;
- a new QR code is generated in a high-quality resolution with the same content;
- this new QR code is drawn with black edges on top of a white background;
- brightness is set to max to help scan the code;
- optionally, you can give this qr code a title and save it to use later.

*Note: I used AI to help create this app.*
