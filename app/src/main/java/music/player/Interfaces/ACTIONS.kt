package music.player.Interfaces


/**
 * Created by Huzaifa Asif on 6/3/2020.
 * Symtera Technologies pvt ltd
 * huzaifano1@hotmail.com
 **/
open interface ACTIONS {
    companion object {
        const val FOREGROUND_SERVICE = 101
        const val INIT_ACTION = "action.init"
        const val MAIN_ACTION = "action.main"
        const val NEXT_ACTION = "action.next"
        const val PLAY_ACTION = "action.play"
        const val PREV_ACTION = "action.prev"
        const val STARTFOREGROUND_ACTION = "action.startforeground"
        const val STOPFOREGROUND_ACTION = "action.stopforeground"
    }
}