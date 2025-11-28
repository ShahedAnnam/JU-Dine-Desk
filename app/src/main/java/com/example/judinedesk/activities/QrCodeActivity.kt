package com.example.judinedesk.activities

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.judinedesk.R
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

class QrCodeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_code)

        val qrImage = findViewById<ImageView>(R.id.qrImage)
        val qrString = intent.getStringExtra("qr_string") ?: "EMPTY"

        qrImage.setImageBitmap(generateQR(qrString))
    }

    private fun generateQR(text: String): Bitmap {
        val size = 600
        val bits = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size)
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size){
            for (y in 0 until size){
                bmp.setPixel(x,y, if(bits[x,y]) Color.BLACK else Color.WHITE)
            }
        }
        return bmp
    }
}
