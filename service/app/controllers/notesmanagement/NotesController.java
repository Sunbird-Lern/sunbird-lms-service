package controllers.notesmanagement;

import com.fasterxml.jackson.databind.JsonNode;
import controllers.BaseController;
import controllers.notesmanagement.validator.NoteRequestValidator;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.Request;
import play.mvc.Http;
import play.mvc.Result;

import java.util.concurrent.CompletionStage;

/**
 * Controller class to handle Notes related operation such as create, read/get, search, update and
 * delete
 */
public class NotesController extends BaseController {

  /**
   * Method to create Note
   *
   * @return CompletionStage<Result>
   */
  public CompletionStage<Result> createNote(Http.Request httpRequest) {
    return handleRequest(
        ActorOperations.CREATE_NOTE.getValue(),
        httpRequest.body().asJson(),
        (request) -> {
          new NoteRequestValidator().validateNote((Request) request);
          return null;
        }, 
            httpRequest);
  }

  /**
   * Method to update the note
   *
   * @param noteId
   * @return CompletionStage<Result>
   */
  public CompletionStage<Result> updateNote(String noteId, Http.Request httpRequest) {
    JsonNode requestData = httpRequest.body().asJson();
    return handleRequest(
        ActorOperations.UPDATE_NOTE.getValue(),
        requestData,
        (request) -> {
          new NoteRequestValidator().validateNoteId(noteId);
          return null;
        },
        noteId,
        JsonKey.NOTE_ID,
            httpRequest);
  }

  /**
   * Method to get the note details
   *
   * @param noteId
   * @return CompletionStage<Result>
   */
  public CompletionStage<Result> getNote(String noteId, Http.Request httpRequest) {
    return handleRequest(
        ActorOperations.GET_NOTE.getValue(),
        (request) -> {
          new NoteRequestValidator().validateNoteId(noteId);
          return null;
        },
        noteId,
        JsonKey.NOTE_ID,
            httpRequest);
  }

  /**
   * Method to search note
   *
   * @return
   */
  public CompletionStage<Result> searchNote(Http.Request httpRequest) {
    return handleRequest(ActorOperations.SEARCH_NOTE.getValue(), httpRequest.body().asJson(), httpRequest);
  }

  /**
   * Method to delete the note
   *
   * @param noteId
   * @return CompletionStage<Result>
   */
  public CompletionStage<Result> deleteNote(String noteId, Http.Request httpRequest) {
    return handleRequest(
        ActorOperations.DELETE_NOTE.getValue(),
        (request) -> {
          new NoteRequestValidator().validateNoteId(noteId);
          return null;
        },
        noteId,
        JsonKey.NOTE_ID,
            httpRequest);
  }
}
