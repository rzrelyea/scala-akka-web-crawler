import com.fasterxml.jackson.core.JsonParseException
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}
import play.api.libs.json._


class JsonParsingSpec
  extends Matchers
  with FlatSpecLike
  with BeforeAndAfterAll {

  "A json parser" should "be able to parse valid json" in {
    val input = """{
                  	"foo": "bar",
                  	"tags": [1,2,3],
                  		"nested": [{
                  		"fooz": "baz",
                  		"id": 1
                  	}]
                  }"""
    val baz: JsValue = Json.parse(input)
  }

  it should "choke on invalid json" in {
    val input = """{
                  	"foo": "bar",
                  	"tags": [1,2,3],
                  		"nested": [{
                  		"fooz": unquoted text,
                  		"id": 1
                  	}]
                  }"""
    intercept[JsonParseException] {
      val baz: JsValue = Json.parse(input)
    }
  }
}
