package Enums;

public enum PlayerActions {
  FORWARD(1),
  STOP(2),
  STARTAFTERBURNER(3),
  STOPAFTERBURNER(4),
  FIRETORPEDOES(5),
  FIRE_SUPERNOVA(6),
  DETONATE_SUPERNOVA(7),
  FIRE_TELEPORT(8),
  TELEPORT(9),
  ACTIVATE_SHIELD(10);

  public final Integer value;

  private PlayerActions(Integer value) {
    this.value = value;
  }
}
