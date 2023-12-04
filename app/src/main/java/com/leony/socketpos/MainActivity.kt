package com.leony.socketpos

import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.vfi.bri_ecr_lib.BriEcrLib
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.Socket
import java.net.UnknownHostException

class MainActivity : AppCompatActivity() {
    private lateinit var txtStatus: TextView
    private lateinit var edtIpAddress: EditText
    private lateinit var edtIpPort: EditText
    private lateinit var btnConnect: AppCompatButton
    private lateinit var btnReqPayment: AppCompatButton
    private lateinit var btnResPayment: AppCompatButton


    private var serverAddress: String = "192.168.1.8"
    private var serverPort = 9001

    private lateinit var briEcrLib: BriEcrLib

    private var clientThread: ClientsThread? = null
    private var thread: Thread? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        txtStatus = findViewById(R.id.txtStatus)
        edtIpAddress = findViewById(R.id.edtIpAddress)
        edtIpPort = findViewById(R.id.edtIpPort)
        btnConnect = findViewById(R.id.btnConnect)
        btnReqPayment = findViewById(R.id.btnRequestPayment)
        btnResPayment = findViewById(R.id.btnRespPayment)

        briEcrLib = BriEcrLib(this)

        btnConnect.setOnClickListener {
            Thread{
                serverAddress = edtIpAddress.text.toString()
                serverPort = edtIpPort.text.toString().toInt()
                val isOpenSocket = briEcrLib.openSocket(serverAddress, serverPort , true)
                Log.e("PosEcr","isOpenSocket: $isOpenSocket")
            }.start()
            clientThread = ClientsThread()
            thread = Thread(clientThread)
            thread!!.start()
        }

        btnReqPayment.setOnClickListener {
            Log.e("PosEcr","isConnected: ${briEcrLib.isConnected()}")
            Thread {
/*
    Request by JsonString
 */
//                val req ="{\n" +
//                        "  \"version\": \"${briEcrLib.getVersion()}\",\n" +
//                        "  \"transType\": \"SALE\",\n" +
//                        "  \"transAmount\": \"${request.transData.amount}\",\n" +
//                        "  \"invoiceNo\": \"${request.traceNumber}\",\n" +
//                        "  \"transAddAmount\": \"0\",\n" +
//                        "  \"cardNumber\": \"\",\n" +
//                        "  \"filler\": \"\"\n" +
//                        "}".trim()
                //OR
                val request ="{\"TransType\":\"01\",\"TransAmount\":\"1\",\"InvoiceNo\":\"0001\",\"TransAddAmount\":\"O\",\"CardNumber\":\"\",\"Filler\":\"\"}".trim()
                val packRequest = briEcrLib.packRequest(request)
                val send = briEcrLib.sendSocket(packRequest)

                Log.e("PosEcr","isSendSocket Success? : $send")
            }.start()
        }

        btnResPayment.setOnClickListener {
            Thread{
                briEcrLib.openSocket(serverAddress, serverPort, true)
            }.start()
            if (!briEcrLib.isConnected()) {
                Toast.makeText(applicationContext, "Server is not connected", Toast.LENGTH_SHORT)
                    .show()
            } else {
                val recvSocket = briEcrLib.recvSocket()
                val resMessage = briEcrLib.parseResponse(recvSocket)
                txtStatus.text = resMessage
                Log.e("PosEcr","Receiver ECR: $recvSocket")
            }
        }
    }

    fun String.decodeHex(): ByteArray {
        check(length % 2 == 0) { "Must have an even length" }

        return chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()
    }


    inner class ClientsThread : Runnable {
        private var socket: Socket? = null
        private var input: BufferedReader? = null
        override fun run() {
            try {
                val serverAddr: InetAddress = InetAddress.getByName(serverAddress)
                socket = Socket(serverAddr, serverPort)
                while (!Thread.currentThread().isInterrupted) {
                    input = BufferedReader(InputStreamReader(socket!!.getInputStream()))
                    var message: String = input!!.readLine()
                    if (null == message || "Disconnect".contentEquals(message)) {
                        break
                    }
                    val recvSocket = briEcrLib.recvSocket()
                    val resMessage = briEcrLib.parseResponse(recvSocket)
                    Log.e("PosEcr","Msg From Server: $resMessage")
                }
            } catch (e: UnknownHostException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}