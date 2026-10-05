# QRCodeDisplayer

The goal of this android app is to make QR codes easier to scan.
I had some issues trying to scan a parking pass at a machine and thought of this solution.

The app works like this:

- you open an image that contains a QR code in your phone and "share" that image to this app;
  - you now can also select the image directly from within the app!
- the app first uses [zxing-cpp](https://github.com/zxing-cpp/zxing-cpp) to decode the code and if that fails it tries [BoofCV](https://github.com/lessthanoptimal/BoofCV);
- a new QR code is generated in a high-quality resolution with the same content;
- this new QR code is drawn with black edges on top of a white background;
- you then have the options to:
  1. enter display mode, which keeps brightness to max and the screen awake to help with scanning
  2. add a title and save this new QR code
  3. see the contents of this code and copy it

*Note: I used AI to help create this app.*

## Example workflow

<table>
  <tr>
    <td><img src="https://github.com/user-attachments/assets/74fee7b1-dd9a-4e45-beca-0b218317b5bd" width="250"/></td>
    <td><img src="https://github.com/user-attachments/assets/a74f861b-b36a-4deb-bb37-759e93c0e413" width="250"/></td>
    <td><img src="https://github.com/user-attachments/assets/fd6616cc-5409-4ac1-879e-cbef6647672a" width="250"/></td>
  </tr>
  <tr>
    <td align="center">Original QR code (<a href="https://support.gametize.com/hc/en-gb/articles/360008688132--QR-Code-Challenge-Displaying-QR-code-for-scanning">ref</a>)</td>
    <td align="center">Share menu</td>
    <td align="center">Improved QR code for display</td>
  </tr>
</table>

## How to install

### 1. F-Droid

The app is available on F-Droid, so you can install and receive automatic updates directly from there!  
[<img width="141" height="42" alt="Get it on F-Droid" src="https://github.com/user-attachments/assets/1948537f-59e7-424b-94b4-4616ad37ef29" />](https://f-droid.org/packages/app.cbdm.qrcodedisplayer/)

### 2. Straight from GitHub

You can also grab the apk directly from the [releases page](https://github.com/cbdm/QRCodeDisplayer/releases).  
If you choose this option, you could use [Obtainium](https://github.com/ImranR98/Obtainium) to receive automatic updates.  
[<img width="141" height="42" alt="Get it on Obtainium" src="https://github.com/user-attachments/assets/47f24b05-5255-481c-b415-84dbcc484baa" />](https://apps.obtainium.imranr.dev/redirect?r=obtainium://app/%7B%22id%22:%20%22app.cbdm.qrcodedisplayer%22,%20%22url%22:%20%22https://github.com/cbdm/QRCodeDisplayer/%22,%20%22author%22:%20%22cbdm%22,%20%22name%22:%20%22QR%20Code%20Displayer%22%7D)
