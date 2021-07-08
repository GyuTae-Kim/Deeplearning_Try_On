package kr.ac.kumoh.s20160001.shoppingmall

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_detail.*
import kr.ac.kumoh.s20160001.shoppingmall.databinding.ActivityDetailBinding
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class DetailActivity : AppCompatActivity() {

    private lateinit var model: ViewModel
    private val mAdapter = ProductAdapter()
    private lateinit var binding: ActivityDetailBinding
    val CAMERA = arrayOf(Manifest.permission.CAMERA)
    val STORAGE = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    private var imageUri: Uri?=null
    val REQUEST_IMAGE_CAPTURE = 1
    val SELECT_IMAGE_GALLERY = 100
    lateinit var currentPhotoPath: String
    var id: String? = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)
        binding = ActivityDetailBinding.inflate(layoutInflater)

        binding.recommendList.apply {
            layoutManager = LinearLayoutManager(this@DetailActivity,LinearLayoutManager.HORIZONTAL,false)
            setHasFixedSize(true)
            itemAnimator = DefaultItemAnimator()
            adapter = mAdapter
        }


        model = ViewModelProvider(this,
            ViewModelProvider.AndroidViewModelFactory(application))
            .get(ViewModel::class.java)

        model.reco_list.observe(this, Observer<ArrayList<ViewModel.reco_Product>>{
            mAdapter.notifyDataSetChanged()
        })
        model.getReco()


        val Gprice = intent.getStringExtra("price")
        val Gname = intent.getStringExtra("name")
        val Gid = intent.getStringExtra("id")

        id = Gid
        binding.textView.setText(Gname)
        binding.textView2.setText(Gprice)
        model.setImg(id,binding.imageView)


        setContentView(binding.root)
        binding.button2.setOnClickListener {
            val mDial = LayoutInflater.from(this).inflate(R.layout.custom_dialog,null)
            var builder = AlertDialog.Builder(this)
            builder.setTitle("사진선택")
            builder.setView(mDial)
            builder.show()
            val cam = mDial.findViewById<ImageButton>(R.id.btn_cam)
            val gall = mDial.findViewById<ImageButton>(R.id.btn_gallery)

            cam.setOnClickListener {
                if (checkPermission(CAMERA)) {
                    Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
                        if (takePictureIntent.resolveActivity(this.packageManager) != null) {
                            // 찍은 사진을 그림파일로 만들기
                            val photoFile: File? =
                                try {
                                    createImageFile()
                                } catch (ex: IOException) {
                                    Log.d("TAG", "그림파일 만드는도중 에러생김")
                                    null
                                }

                            // 그림파일을 성공적으로 만들었다면 onActivityForResult로 보내기
                            photoFile?.also {
                                val photoURI: Uri = FileProvider.getUriForFile(
                                    this, "kr.ac.kumoh.s20160001.shoppingmall.fileprovider", it
                                )
                                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                            }
                        }
                    }
                }
            }

            gall.setOnClickListener {
                if (checkPermission(STORAGE)){
                    val gallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
                    startActivityForResult(gallery, SELECT_IMAGE_GALLERY)
                }

            }
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val viewer = findViewById<ImageView>(R.id.selectedImg)
        if (resultCode == RESULT_OK && requestCode == SELECT_IMAGE_GALLERY) {
            imageUri = data?.data
            intent = Intent(this, SelectActivity::class.java).apply {
                putExtra("Uri",imageUri.toString())
                putExtra("id",id)
            }
            startActivity(intent)
        }
        else if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            val file = File(currentPhotoPath)
            imageUri = Uri.fromFile(file)
            intent = Intent(this, SelectActivity::class.java).apply {
                putExtra("Uri",imageUri.toString())
                putExtra("id",id)
            }
            startActivity(intent)
        }
    }

    fun checkPermission(permissions: Array<out String>): Boolean
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (permission in permissions) {
                if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, permissions, REQUEST_IMAGE_CAPTURE)
                    return false;
                }
            }
        }

        return true;
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray)
    {
        when(requestCode) {
            REQUEST_IMAGE_CAPTURE -> {
                for (grant in grantResults) {
                    if (grant != PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(this, "권한을 승인해 주세요.", Toast.LENGTH_LONG).show()
                    }
                }
            }

        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
    }

    inner class ProductAdapter: RecyclerView.Adapter<ProductAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val productImg = itemView.findViewById<ImageView>(R.id.image)
        }

        override fun getItemCount(): Int {
            return model.getRecoSize()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductAdapter.ViewHolder {
            val view = layoutInflater.inflate(
                    R.layout.cardview,
                    parent,
                    false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ProductAdapter.ViewHolder, position: Int) {
            model.setImg(model.getRecoProduct(position).id,holder.productImg)
            holder.itemView.setOnClickListener(){
                val intent = Intent(holder.itemView?.context, DetailActivity::class.java)
                intent.putExtra("name",model.getRecoProduct(position).name )
                intent.putExtra("price",model.getRecoProduct(position).price )
                intent.putExtra("id",model.getRecoProduct(position).id)
                ContextCompat.startActivity(holder.itemView.context,intent,null)
            }
        }
    }

}