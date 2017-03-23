package com.advancedspark.serving.prediction.python

import scala.util.Failure
import scala.util.Success
import scala.util.Try
import scala.util.parsing.json.JSON

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.cloud.netflix.hystrix.EnableHystrix
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController

import com.soundcloud.prometheus.hystrix.HystrixPrometheusMetricsPublisher

import io.prometheus.client.hotspot.StandardExports
import io.prometheus.client.spring.boot.EnablePrometheusEndpoint
import io.prometheus.client.spring.boot.EnableSpringBootMetricsCollector


@SpringBootApplication
@RestController
@EnableHystrix
@EnablePrometheusEndpoint
@EnableSpringBootMetricsCollector	
class PredictionService {
  HystrixPrometheusMetricsPublisher.register("prediction_python")
  new StandardExports().register()
  
  val responseHeaders = new HttpHeaders();

  @RequestMapping(path=Array("/update-python/{namespace}/{name}/{version}"),
                  method=Array(RequestMethod.POST),
                  produces=Array("application/json; charset=UTF-8"))
  def updateSource(@PathVariable("namespace") namespace: String,
                   @PathVariable("name") name: String,
                   @PathVariable("version") version: String,                   
                   @RequestBody source: String): 
      ResponseEntity[String] = {
    Try {
      System.out.println(s"Updating source for ${namespace}/${name}/${version}:\n${source}")

      // Write the new java source to local disk
      val path = new java.io.File(s"store/${namespace}/${name}/${version}/")
      if (!path.isDirectory()) {
        path.mkdirs()
      }

      val file = new java.io.File(s"store/${namespace}/${name}/${version}/${name}.py")
      if (!file.exists()) {
        file.createNewFile()
      }

      val fos = new java.io.FileOutputStream(file)
      fos.write(source.getBytes())

      new ResponseEntity[String](source, responseHeaders, HttpStatus.OK)
    } match {
      case Failure(t: Throwable) => {
        val responseHeaders = new HttpHeaders();
        new ResponseEntity[String](s"""${t.getMessage}:\n${t.getStackTrace().mkString("\n")}""", responseHeaders,
          HttpStatus.BAD_REQUEST)
      }
      case Success(response) => response      
    }
  }
 
  @RequestMapping(path=Array("/evaluate-python/{namespace}/{name}/{version}"),
                  method=Array(RequestMethod.POST),
                  produces=Array("application/json; charset=UTF-8"))
  def evaluateSource(@PathVariable("namespace") namespace: String,
                     @PathVariable("name") name: String, 
                     @PathVariable("version") version: String,
                     @RequestBody inputJson: String): 
      ResponseEntity[String] = {
    Try {
      val inputs = JSON.parseFull(inputJson).get.asInstanceOf[Map[String,Any]]

      val filename = s"${name}.py"

      val result = new PythonSourceCodeEvaluationCommand(name, namespace, version, filename, inputJson, "fallback", 10000, 20, 10).execute()

      new ResponseEntity[String](s"${result}", responseHeaders,
           HttpStatus.OK)
    } match {
      case Failure(t: Throwable) => {
        new ResponseEntity[String](s"""${t.getMessage}:\n${t.getStackTrace().mkString("\n")}""", responseHeaders,
          HttpStatus.BAD_REQUEST)
      }
      case Success(response) => response
    }   
  }
}

object PredictionServiceMain {
  def main(args: Array[String]): Unit = {
    SpringApplication.run(classOf[PredictionService])
  }
}
