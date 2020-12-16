package com.example.test_pdf

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.tom_roush.pdfbox.util.PDFBoxResourceLoader
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.*
import java.io.IOException
import java.util.*


class MainActivity : AppCompatActivity() {

    lateinit var pdfFile : Uri
    lateinit var pdfEncoded : String
    companion object {
        private const val PDF_SELECTION_CODE = 99
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        PDFBoxResourceLoader.init(applicationContext);

        btn_choose.setOnClickListener {
            selectPdfFromStorage()
        }

        btn_connect.setOnClickListener {
            checking()
        }

        btn_send.setOnClickListener {
            postPdf()
        }

        btn_receive.setOnClickListener{
           receiveDoc()
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == PDF_SELECTION_CODE && resultCode == Activity.RESULT_OK && data != null){
            val selectedPdfFromStorage = data.data
            println("URI of  PDF file is $selectedPdfFromStorage")
            if (selectedPdfFromStorage != null) {
                tv_pdf.text = selectedPdfFromStorage.toString()
                pdfFile = selectedPdfFromStorage
                pdf_viewer.fromUri(selectedPdfFromStorage).load()
            }
        }
    }


    private fun selectPdfFromStorage() {
        Toast.makeText(this, "selectPDF", Toast.LENGTH_LONG).show()
        val browseStorage = Intent(Intent.ACTION_GET_CONTENT)
        browseStorage.type = "application/pdf"
        browseStorage.addCategory(Intent.CATEGORY_OPENABLE)
        startActivityForResult(
            Intent.createChooser(browseStorage, "Select PDF"), PDF_SELECTION_CODE)
    }

    private fun checking(){
        val client = OkHttpClient()

        val request = Request.Builder().url("http://192.168.29.216:5000/").build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                tv_connect.text = "Failed"
            }
            override fun onResponse(call: Call, response: Response) {
                runOnUiThread(object : Runnable{
                    override fun run() {
                        tv_connect.setText(response.body?.string())
                    }
                })
            }
        })
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun postPdf(){
        val inputStream = this.contentResolver.openInputStream(pdfFile)
        val pdfInBytes : ByteArray? = inputStream?.available()?.let { ByteArray(it) }
        inputStream?.read(pdfInBytes)
        pdfEncoded = Base64.getEncoder().encodeToString(pdfInBytes)
        //tv_post.text = pdfEncoded
        uploadDocument()
    }

    private fun uploadDocument(){
        val  client = OkHttpClient()
        val body = FormBody.Builder().add("value",pdfEncoded).add("pdfName",et_set.text.toString()).build()
        val request = Request.Builder().url("http://192.168.29.216:5000/pdf").post(body).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { Runnable { tv_receive.text = "failed" } }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread(object : Runnable{
                    override fun run() {
                        tv_receive.text = "${response.body?.string()}"
                    }
                })
            }
        })
    }

    private fun receiveDoc(){
        val client = OkHttpClient()

        val request = Request.Builder().url("http://192.168.29.216:5000/return").build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                tv_receive.text = "Failed"
            }
            override fun onResponse(call: Call, response: Response) {
                runOnUiThread(object : Runnable{
                    override fun run() {
                        tv_receive.setText(response.body?.string())
                    }
                })
            }
        })
    }
}
