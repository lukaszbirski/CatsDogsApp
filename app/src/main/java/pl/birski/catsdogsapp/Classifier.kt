package pl.birski.catsdogsapp

import android.content.Context
import android.content.res.AssetManager
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.PriorityQueue
import kotlin.Comparator
import kotlin.collections.ArrayList

class Classifier(
    private val context: Context
) {
    private var interpreter: Interpreter
    private var lableList: List<String>
    private val INPUT_SIZE = 224
    private val PIXEL_SIZE: Int = 3
    private val IMAGE_MEAN = 0
    private val IMAGE_STD = 255.0f
    private val MAX_RESULTS = 3
    private val THRESHOLD = 0.4f

    private val modelPath = "converted_model.tflite"
    private val labelPath = "label.txt"

    init {
        val options = Interpreter.Options()
        val assetManager = context.assets
        options.setNumThreads(5)
        options.setUseNNAPI(true)
        interpreter = Interpreter(loadModelFile(assetManager), options)
        lableList = loadLabelList(assetManager)
    }

    private fun loadModelFile(assetManager: AssetManager): MappedByteBuffer {
        val fileDescriptor = assetManager.openFd(modelPath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun loadLabelList(assetManager: AssetManager) =
        assetManager.open(labelPath).bufferedReader().useLines { it.toList() }

    fun recognizeImage(bitmap: Bitmap): List<Recognition> {
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, false)
        val byteBuffer = convertBitmapToByteBuffer(scaledBitmap)
        val result = Array(1) { FloatArray(lableList.size) }
        interpreter.run(byteBuffer, result)
        return getSortedResult(result)
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(4 * INPUT_SIZE * INPUT_SIZE * PIXEL_SIZE)
        byteBuffer.order(ByteOrder.nativeOrder())
        val intValues = IntArray(INPUT_SIZE * INPUT_SIZE)

        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        var pixel = 0
        for (i in 0 until INPUT_SIZE) {
            for (j in 0 until INPUT_SIZE) {
                val input = intValues[pixel++]

                byteBuffer.putFloat((((input.shr(16) and 0xFF) - IMAGE_MEAN) / IMAGE_STD))
                byteBuffer.putFloat((((input.shr(8) and 0xFF) - IMAGE_MEAN) / IMAGE_STD))
                byteBuffer.putFloat((((input and 0xFF) - IMAGE_MEAN) / IMAGE_STD))
            }
        }
        return byteBuffer
    }

    private fun getSortedResult(labelProbArray: Array<FloatArray>): List<Recognition> {
        val pq = PriorityQueue(
            MAX_RESULTS,
            Comparator<Recognition> { (_, _, confidence1), (_, _, confidence2) ->
                confidence1.compareTo(confidence2) * -1
            }
        )

        for (i in lableList.indices) {
            val confidence = labelProbArray[0][i]
            if (confidence >= THRESHOLD) {
                pq.add(
                    Recognition(
                        "" + i,
                        if (lableList.size > i) lableList[i] else "Unknown",
                        confidence
                    )
                )
            }
        }

        val recognitions = ArrayList<Recognition>()
        val recognitionsSize = pq.size.coerceAtMost(MAX_RESULTS)
        for (i in 0 until recognitionsSize) {
            pq.poll()?.let { recognitions.add(it) }
        }
        return recognitions
    }
}
