package net.ladstatt.apps.isight

import java.io.File
import java.io.FileInputStream
import org.opencv.core.Core
import org.opencv.core.MatOfRect
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.highgui.Highgui
import org.opencv.objdetect.CascadeClassifier
import javafx.application.Application
import javafx.scene.Group
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.stage.Stage
import org.opencv.highgui.VideoCapture
import org.opencv.core.Mat

object HelloOpenCVUsingIsight {

  def main(args: Array[String]): Unit = {
    Application.launch(classOf[HelloOpenCVUsingIsight], args: _*)
  }
}

// this can get more complicated if you use a "native" approach
// using a jni based solution
// see http://iharder.sourceforge.net/current/macosx/imagesnap/
trait ImageSource {

  def sourceImage: Either[Exception, Mat] = {
    val videocapture = new VideoCapture(0)
    assert(videocapture.isOpened())
    if (videocapture.grab) {
      val image = new Mat()
      while (videocapture.read(image) == false) {
        Thread.sleep(10)
        println("waiting for camera ...")
      } 
      Right(image)
    } else {
      Left(new RuntimeException("Couldn't grab image!"))
    }
  }

}

trait FaceScanner {

  def scanFace(image: Mat): File = {

    // Create a face detector from the cascade file in the resources
    // directory.
    val faceDetector = new CascadeClassifier(getClass().getResource("/lbpcascade_frontalface.xml").getPath())

    // Detect faces in the image.
    // MatOfRect is a special container class for Rect.
    val faceDetections = new MatOfRect()
    faceDetector.detectMultiScale(image, faceDetections)

    // Draw a bounding box around each face.
    for (rect <- faceDetections.toArray()) {
      Core.rectangle(image, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(0, 255, 0))
    }

    // Save the visualized detection.
    val fileName = "faceDetection.png"
    Highgui.imwrite(fileName, image)
    val f = new File(fileName)
    f.deleteOnExit()
    f
  }

}

class HelloOpenCVUsingIsight extends javafx.application.Application with ImageSource with FaceScanner {

  override def init(): Unit = {
    // important to have this statement on the "right" thread
    System.load(new File("/opt/local/share/OpenCV/java/libopencv_java244.dylib").getAbsolutePath())
  }

  override def start(stage: Stage): Unit = {

    stage.setTitle("Webcam snapshot with face detection")

    sourceImage match {
      case Left(e) => throw e
      case Right(mat) => {
        val group = new Group
        val imageView = new ImageView(new Image(new FileInputStream(scanFace(mat))))
        group.getChildren.add(imageView)
        val scene = new Scene(group)

        stage.setScene(scene)
        stage.show()

      }
    }
  }

}