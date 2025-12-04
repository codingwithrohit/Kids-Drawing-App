package com.example.kidsdrawingapp


import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat


class MainActivity : AppCompatActivity() {

    private var drawingView: DrawingView? = null
    private var ibCurrentPaint: ImageButton? = null
    private var imgBackground: ImageView? = null
    private var currentBrushSize = 10f

    val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ){
        isGranted: Boolean ->
        if(isGranted){
            val pickImageIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            openGalleryLauncher.launch(pickImageIntent)
        }else{
            //User permanently denied
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this, getStoragePermission())) {
                showSettingsDialog()
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val openGalleryLauncher  = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ){
        result ->
        if(result.resultCode == RESULT_OK && result.data != null){
            val data: Intent? = result.data
            val uri: Uri? = data?.data
            uri?.let {
                imgBackground?.setImageURI(it) // show in ImageView
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        //Setting up drawing view
        drawingView = findViewById(R.id.drawing_view)
        drawingView?.setSizeForBrush(currentBrushSize)

        val linearLayout = findViewById<LinearLayout>(R.id.ll_paint_colors)
        //Changing the appearance of the selected color
        ibCurrentPaint = linearLayout.getChildAt(0) as ImageButton
        ibCurrentPaint!!.setImageDrawable(
            ContextCompat.getDrawable(this, R.drawable.color_pallet_pressed)
        )

        //Setting up image picker launcher
        imgBackground  = findViewById(R.id.iv_background_image)
        val ibGallery: ImageButton = findViewById(R.id.ib_select_image)
        ibGallery.setOnClickListener {
            requestPermission()
        }
        //Setting up brush size dialog on click of brush button
        val ibBrush: ImageButton = findViewById(R.id.ib_brush)
        ibBrush.setOnClickListener{
            showBrushSizeDialog()
        }

        //Setting up undo button on click
        val ibUndo: ImageButton = findViewById(R.id.ib_undo)
        ibUndo.setOnClickListener{
            drawingView?.undoPath()
        }
        //Setting up redo button on click
        val redo: ImageButton = findViewById(R.id.ib_redo)
        redo.setOnClickListener{
            drawingView?.redoPath()
        }
        //Setting up eraser button on click
        val ibEraser: ImageButton = findViewById(R.id.ib_eraser)
        ibEraser.setOnClickListener{
            drawingView?.erasePath()
        }
        //Setting up reset button on click
        val ibReset: ImageButton = findViewById(R.id.ib_reset_image)
        ibReset.setOnClickListener{
            imgBackground?.setImageDrawable(null)
        }
        //Setting up save button on click
        val ibSave: ImageButton = findViewById(R.id.ib_save)
        ibSave.setOnClickListener {
            val flDrawingView: FrameLayout = findViewById(R.id.fl_drawing_view)
            val bitmap = getBitmapFromView(flDrawingView)
            saveBitmapToGallery(bitmap)
        }

    }

    private fun showBrushSizeDialog(){
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Brush size: ")
        val smallBtn: ImageButton = brushDialog.findViewById(R.id.ib_small_brush)
        val mediumBtn: ImageButton = brushDialog.findViewById(R.id.ib_medium_brush)
        val largeBtn: ImageButton = brushDialog.findViewById(R.id.ib_large_brush)

        fun updateSelection() {
            smallBtn.setBackgroundResource(
                if (currentBrushSize == 10f) R.drawable.selected_brush_bg
                else R.drawable.normal_brush_bg
            )
            mediumBtn.setBackgroundResource(
                if (currentBrushSize == 20f) R.drawable.selected_brush_bg
                else R.drawable.normal_brush_bg
            )
            largeBtn.setBackgroundResource(
                if (currentBrushSize == 30f) R.drawable.selected_brush_bg
                else R.drawable.normal_brush_bg
            )
        }
        updateSelection()   // highlight default or last selected brush

        smallBtn.setOnClickListener {
            currentBrushSize = 10f
            drawingView?.setSizeForBrush(10f)
            updateSelection()
            brushDialog.dismiss()
        }

        mediumBtn.setOnClickListener {
            currentBrushSize = 20f
            drawingView?.setSizeForBrush(20f)
            updateSelection()
            brushDialog.dismiss()
        }

        largeBtn.setOnClickListener {
            currentBrushSize = 30f
            drawingView?.setSizeForBrush(30f)
            updateSelection()
            brushDialog.dismiss()
        }
        brushDialog.show()
    }

    fun paintClicked(view: View){
        if(view !== ibCurrentPaint){
            val imageButton = view as ImageButton
            val colorTag = imageButton.tag.toString()
            drawingView?.setColor(colorTag)

            imageButton.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.color_pallet_pressed)
            )
            ibCurrentPaint?.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.pallet_colors)
            )
            ibCurrentPaint = view
        }
    }

    private fun showRationaleDialog(title: String, message: String){
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setNegativeButton("Cancel"){dialog, _->
                Toast.makeText(this, "Cancel", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setPositiveButton("OK"){
                dialog,_ ->
                requestPermissionLauncher.launch(getStoragePermission())
                dialog.dismiss()
            }
            .setCancelable(false)
        val alertDialog: AlertDialog = builder.create()
        alertDialog.show()
    }

    private fun requestPermission(){
        val permission = getStoragePermission()
        when{
            ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_MEDIA_IMAGES)
                    == PackageManager.PERMISSION_GRANTED -> {
                val pickImageIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                Toast.makeText(this, "Opening gallery...", Toast.LENGTH_SHORT).show()
                openGalleryLauncher.launch(pickImageIntent)
            }
            ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.READ_EXTERNAL_STORAGE) -> {
                showRationaleDialog("Kids Drawing App", "Kids Drawing App needs access to gallery to pick images.")
            }
            else -> {
                requestPermissionLauncher.launch(permission)
            }
        }
    }
    private fun getStoragePermission(): String{
        return if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            Manifest.permission.READ_MEDIA_IMAGES
        }
        else{
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    private fun getBitmapFromView(view: View) : Bitmap{
        val returnedBitmap = createBitmap(view.width, view.height)
        val canvas = Canvas(returnedBitmap)
        val bgDrawable = view.background
        if(bgDrawable != null){
            bgDrawable.draw(canvas)
        }
        else{
            canvas.drawColor(getColor(R.color.white))
        }
        view.draw(canvas)
        return returnedBitmap
    }

    private fun saveBitmapToGallery(bitmap: Bitmap) {
        val filename = "KidsDrawingApp_${System.currentTimeMillis()}.png"

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/KidsDrawingApp")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        uri?.let {
            try {
                val outputStream = contentResolver.openOutputStream(uri)
                outputStream?.use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                } ?: run {
                    Toast.makeText(this, "Failed to open output stream", Toast.LENGTH_SHORT).show()
                    return
                }

                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                contentResolver.update(uri, contentValues, null, null)

                Toast.makeText(this, "Saved to Gallery", Toast.LENGTH_SHORT).show()

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Error saving image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showSettingsDialog(){
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Permission Required")
            .setMessage("You have permanently denied the permission. \n\n" +
            "Please open setting and enable it to continue using this feature.")
            .setPositiveButton("Open Setting"){
                _,_->
                val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", packageName, null)
                )
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .setCancelable(false)
            .show()
    }

}




