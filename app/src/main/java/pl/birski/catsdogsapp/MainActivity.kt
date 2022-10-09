package pl.birski.catsdogsapp

import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import pl.birski.catsdogsapp.databinding.ActivityMainBinding
import java.util.Locale

class MainActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var binding: ActivityMainBinding

    private lateinit var classifier: Classifier

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)

        initView()
        classifier = Classifier(this)

        setContentView(binding.root)
    }

    private fun initView() {
        binding.iv1.setOnClickListener(this)
        binding.iv2.setOnClickListener(this)
        binding.iv3.setOnClickListener(this)
        binding.iv4.setOnClickListener(this)
    }

    override fun onClick(view: View) {
        val bitmap = ((view as ImageView).drawable as BitmapDrawable).bitmap
        val result = classifier.recognizeImage(bitmap)
        Toast.makeText(
            this,
            "Recognized: ${result[0].title.uppercase(Locale.ROOT)}",
            Toast.LENGTH_SHORT
        ).show()
    }
}
