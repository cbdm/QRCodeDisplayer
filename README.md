# QRCodeDisplayer

The goal of this android app is to make QR codes easier to scan. I had some issues trying to scan a parking pass at a machine and thought of this solution.

The app works like this:

- you open an image that contains a QR code in your phone and "share" that image to this app;
- the app first uses [zxing-cpp](https://github.com/zxing-cpp/zxing-cpp) to decode the code and if that fails it tries [BoofCV](https://github.com/lessthanoptimal/BoofCV);
- a new QR code is generated in a high-quality resolution with the same content;
- this new QR code is drawn with black edges on top of a white background;
- brightness is set to max to help scan the code;
- optionally, you can give this qr code a title and save it to use later.

*Note: I used AI to help create this app.*

## Example workflow

<table>
  <tr>
    <td><img src="https://github.com/user-attachments/assets/74fee7b1-dd9a-4e45-beca-0b218317b5bd" width="250"/></td>
    <td><img src="https://github.com/user-attachments/assets/a74f861b-b36a-4deb-bb37-759e93c0e413" width="250"/></td>
    <td><img src="https://github.com/user-attachments/assets/212b3682-8210-4cd3-8b4c-87482344b2bf" width="250"/></td>
  </tr>
  <tr>
    <td align="center">Original QR code (<a href="https://support.gametize.com/hc/en-gb/articles/360008688132--QR-Code-Challenge-Displaying-QR-code-for-scanning">ref</a>)</td>
    <td align="center">Share menu</td>
    <td align="center">Improved QR code for display</td>
  </tr>
</table>
