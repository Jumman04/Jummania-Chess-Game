package com.jummania

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jummania.ChessView.Companion.isWhiteTurn


/**
 * ChessController class manages the core logic of a chess game, handling piece movement, validation,
 * and game states such as castling, check, and pawn promotion. It ensures proper interaction between
 * chess pieces and the game board, and includes specific functionality for each piece's movement rules.
 *
 * This controller is responsible for processing moves, verifying turn-based actions, and handling
 * situations such as castling, pawn promotion, and check. It also tracks the state of the game and
 * enforces rules regarding the legality of moves.
 *
 * @property context The application context used for UI interaction or messaging during the game.
 * @property isLightFilled Boolean flag to determine if the light pieces are filled (visual style).
 * @property isDarkFilled Boolean flag to determine if the dark pieces are filled (visual style).
 * @property pieceLightColor The color code for the light-colored pieces.
 * @property pieceDarkColor The color code for the dark-colored pieces.
 *
 * @constructor Creates a ChessController instance with the specified properties, initializing the game board
 *             and other internal variables needed to track the state of the chess game.
 *
 * @see Piece
 * @see Castling
 */
internal class ChessController(
    private val context: Context,
    val isLightFilled: Boolean,
    val isDarkFilled: Boolean,
    private val pieceLightColor: Int,
    val pieceDarkColor: Int
) {

    private var playingOnline: Boolean = false

    // The symbols used for light and dark pieces, depending on whether they are filled or unfilled
    private val filledSymbols =
        arrayOf("♜", "♞", "♝", "♛", "♚", "♝", "♞", "♜") // Symbols for filled pieces (dark pieces)
    private val unfilledSymbols = arrayOf(
        "♖", "♘", "♗", "♕", "♔", "♗", "♘", "♖"
    ) // Symbols for unfilled pieces (light pieces)

    // Initializes light pieces depending on whether they are filled or unfilled
    private val lightPieces = if (isLightFilled) {
        // Create light pieces with filled symbols
        createPieces(filledSymbols, pieceLightColor)
    } else {
        // Create light pieces with unfilled symbols
        createPieces(unfilledSymbols, pieceLightColor)
    }

    // Initializes dark pieces depending on whether they are filled or unfilled
    private val darkPieces = if (isDarkFilled) {
        // Create dark pieces with filled symbols
        createPieces(filledSymbols, pieceDarkColor)
    } else {
        // Create dark pieces with unfilled symbols
        createPieces(unfilledSymbols, pieceDarkColor)
    }


    // A function that will be executed after a revival event (e.g., when a piece is revived or game state is restored).
// The function will be assigned at runtime.
    private lateinit var afterRevival: (() -> Unit)

    private var onlinePlayer: ((String, String) -> Unit)? = null

    // Castling objects for white and black players. These objects track castling status.
    private val whiteCastling =
        Castling()  // White player's castling data (can track rook and king for castling move)
    private val blackCastling =
        Castling()  // Black player's castling data (can track rook and king for castling move)

    // The chessboard is represented as an array of 64 nullable Piece objects (empty spots are `null`).
// Each index corresponds to a square on the chessboard.
    private val chessBoard =
        arrayOfNulls<Piece>(64)  // Chessboard with 64 squares, initially all empty

    // `indices` is a range of valid positions (0 to 63) on the chessboard array.
// This is useful to easily iterate over the chessboard and access pieces.
    private val indices = chessBoard.indices  // Indices of the chessboard (from 0 to 63)


    init {
        // Initialize the chessboard with light pieces (first 8 squares)
        // The pieces are placed in their respective positions based on the `lightPieces` array.
        // `lightPieces` should contain the symbols for each light piece (e.g., rook, knight, etc.)
        for (i in lightPieces.indices) {
            chessBoard[i] =
                lightPieces[i]  // Place each light piece at its respective position on the board (0 to 7)
        }

        // Set the symbol for light pawns. If the pieces are filled (i.e., more stylized), use ♟; otherwise, use ♙.
        val lightPawnSymbol = if (isLightFilled) "♟" else "♙"
        // Place light pawns in the second rank (index 8 to 15)
        for (i in 8 until 16) {
            chessBoard[i] = Piece(
                lightPawnSymbol, pieceLightColor
            )  // Assign pawn symbol and color for the light player
        }

        // Set the symbol for dark pawns. If the pieces are filled, use ♟; otherwise, use ♙.
        val darkPawnSymbol = if (isDarkFilled) "♟" else "♙"
        // Place dark pawns in the second-to-last rank (index 48 to 55)
        for (i in 48 until 56) {
            chessBoard[i] = Piece(
                darkPawnSymbol, pieceDarkColor
            )  // Assign pawn symbol and color for the dark player
        }

        // Initialize the dark pieces (pieces for the dark player) on their starting positions
        for (i in darkPieces.indices) {
            chessBoard[56 + i] = darkPieces[i]  // Place dark pieces from indices 56 to 63
        }
    }


    /**
     * Retrieves the piece at a specific position on the chessboard.
     *
     * @param position The index of the position on the chessboard.
     * @return The piece at the specified position, or null if the position is invalid or empty.
     */
    fun get(position: Int): Piece? {
        return if (position in indices) chessBoard.get(position) else null
    }


    /**
     * Moves a piece from one position on the chessboard to another.
     *
     * @param fromIndex The index of the starting position of the piece.
     * @param toIndex The index of the target position where the piece will be moved.
     * @param piece The piece to be moved.
     */
    private fun movePiece(fromIndex: Int, toIndex: Int, piece: Piece) {
        // Place the piece at the target position
        chessBoard[toIndex] = piece

        // Remove the piece from the starting position (set to null)
        chessBoard[fromIndex] = null
    }


    /**
     * Checks if the given piece is a light-colored piece.
     *
     * @param piece The piece to check
     * @return Boolean - true if the piece is light-colored, false otherwise
     */
    fun isLightPiece(piece: Piece?): Boolean {
        // Check if the piece is not null and if its color matches the light color
        return piece?.color == pieceLightColor
    }


    /**
     * Checks if a given piece is a friend (i.e., belongs to the same player)
     * based on the color of the piece.
     *
     * @param piece The piece to check
     * @param isWhitePiece Boolean indicating if the current player is white
     * @return Boolean - true if the piece belongs to the same player (friend), false otherwise
     */
    private fun isFriend(piece: Piece?, isWhitePiece: Boolean): Boolean {
        // Check if the piece is not null and if its color matches the current player's color
        return piece != null && isLightPiece(piece) == isWhitePiece
    }


    /**
     * Checks if a move to a given position is valid based on the piece at that position.
     * A move is valid if the target position is either empty or occupied by an opponent's piece.
     *
     * @param pos The target position on the board
     * @param isWhitePiece Boolean indicating whether the piece trying to move is white
     * @return Boolean - true if the move is valid, false otherwise
     */
    private fun isValidMove(pos: Int, isWhitePiece: Boolean): Boolean {
        // Get the piece at the target position
        val piece = get(pos)

        // The move is valid if the target position is empty (piece == null)
        // or if the target position is occupied by an opponent's piece
        return piece == null || !isFriend(piece, isWhitePiece)
    }

    /**
     * Handles a piece's movement from one position to another on the chessboard.
     * This method performs the necessary checks for valid moves, including turn validation,
     * piece-specific movement rules, capturing, castling, and pawn promotion.
     *
     * @param fromIndex The starting position (index) of the piece on the chessboard.
     * @param toIndex The destination position (index) where the piece is moving to.
     * @return Boolean indicating whether the move was successful. Returns `true` if the move was valid,
     *         `false` if the move was invalid or failed.
     */
    fun swapTo(fromIndex: Int, toIndex: Int, isOnline: Boolean): Boolean {

        if (!isOnline && fromOnline()) return false

        // Check if both the source and destination indices are valid
        if (fromIndex !in indices || toIndex !in indices) return false

        // Retrieve the piece at the source index
        val fromPiece =
            chessBoard[fromIndex] ?: return false // If there's no piece at the source, return false

        // Check if the turn is valid (is it the player's turn?)
        if (isWhiteTurn != isLightPiece(fromPiece)) {
            message("It's not your turn!")  // Inform the player if it's not their turn
            return true
        }

        // Retrieve the piece at the destination index (if any)
        val toPiece = chessBoard[toIndex]

        // Check if the destination square contains a piece of the same color
        if (isFriend(toPiece, isWhiteTurn)) return false  // Can't land on your own piece


        var kingPosition = chessBoard.indexOfFirst {
            it != null && it.isKing() && it.color == if (isWhiteTurn) pieceLightColor else pieceDarkColor
        }

        /*
        val attackingPosition = findAttacker(kingPosition, isWhiteTurn)
        if (attackingPosition != -1) {
            if (findAttacker(attackingPosition, !isWhiteTurn) == -1) {


            }
        }


        val movePosition = intArrayOf(
            kingPosition - 9,
            kingPosition - 8,
            kingPosition - 7,
            kingPosition + 7,
            kingPosition + 8,
            kingPosition + 9
        )

        var kingCanMoove = true
        for (i in movePosition) {
            if (i !in indices) continue
            //   Log.d("Jjj", "swapTo: $i")
            if (isFriend(chessBoard[i], isWhiteTurn)) {
                continue
            }
            kingCanMoove = isKingMoveAllowed(kingPosition, i, 2, isWhiteTurn)
            if (kingCanMoove) break
        }

        if (!kingCanMoove) showEndDialogue()


         */


        // Initialize flags for Rook and King checks (for castling and rook-specific movement)
        val isRook = fromPiece.isRook()
        val isKing = fromPiece.isKing()

        // Check specific movement rules for each piece type
        when {
            fromPiece.isPawn() -> {
                // Check if the pawn's move is allowed
                if (!isPawnMoveAllowed(fromIndex, toIndex, isWhiteTurn)) {
                    if (isOnline) return false
                    message("The Pawn can only move one square forward.")  // Inform the player of invalid move
                    return true
                }
            }

            fromPiece.isKnight() -> {
                // Check if the knight's move is allowed
                if (!isKnightMoveAllowed(fromIndex, toIndex, isWhiteTurn)) {
                    if (isOnline) return false
                    message("The Knight can only move in an L shape.")  // Inform the player of invalid move
                    return true
                }
            }

            fromPiece.isBishop() -> {
                // Check if the bishop's move is allowed
                if (!isBishopMoveAllowed(fromIndex, toIndex, isWhiteTurn)) {
                    if (isOnline) return false
                    message("The Bishop can only move diagonally.")  // Inform the player of invalid move
                    return true
                }
            }

            isRook -> {
                // Check if the rook's move is allowed
                if (!isRookMoveAllowed(fromIndex, toIndex, isWhiteTurn)) {
                    if (isOnline) return false
                    message("The Rook can only move horizontally or vertically.")  // Inform the player of invalid move
                    return true
                }
            }

            fromPiece.isQueen() -> {
                // Check if the queen's move is allowed
                if (!isQueenMoveAllowed(fromIndex, toIndex, isWhiteTurn)) {
                    if (isOnline) return false
                    message("The Queen can move horizontally, vertically, or diagonally.")  // Inform the player of invalid move
                    return true
                }
            }

            isKing -> {
                // Check if castling is allowed or if the king’s move is valid
                if (isCastled(fromIndex, toIndex, isWhiteTurn, fromPiece, toPiece)) {
                    isWhiteTurn = !isWhiteTurn  // If castling is successful, switch turns
                    sendData()
                    return true
                } else if (!isKingMoveAllowed(fromIndex, toIndex, 2, isWhiteTurn)) {
                    if (isOnline) return false
                    message("The King can only move one square in any direction.")  // Inform the player of invalid move
                    return true
                }
            }
        }

        // Make the move: Move the piece from the source to the destination
        movePiece(fromIndex, toIndex, fromPiece)

        // Check if the move puts the current player's king in check

        if (isCheck(isWhiteTurn) != -1) {
            // Reverse the move if it results in the king being in check
            reverseMove(fromIndex, toIndex, fromPiece, toPiece)
            if (isOnline) return false
            message("Illegal move: You must get out of check and can't put your King in danger.")  // Inform the player of illegal move
            return true
        }


        val opsiteTurn = !isWhiteTurn

        kingPosition = chessBoard.indexOfFirst {
            it != null && it.isKing() && it.color == if (opsiteTurn) pieceLightColor else pieceDarkColor
        }


        val attackingPosition = findAttacker(kingPosition, opsiteTurn)
        if (attackingPosition != -1) {
            val s = findAttacker(attackingPosition, !opsiteTurn)

            if (s == -1) {
                showEndDialogue()/*
                val movePosition = intArrayOf(
                    kingPosition - 9,
                    kingPosition - 8,
                    kingPosition - 7,
                    kingPosition + 7,
                    kingPosition + 8,
                    kingPosition + 9
                )

                var kingCanMoove = false
                for (i in movePosition) {
                    kingCanMoove = isKingMoveAllowed(kingPosition, i, 2, opsiteTurn)
                    if (kingCanMoove) {
                        Log.d("Jjj", "kingCanMoove: $i")
                        break
                    }
                }

                if (!kingCanMoove)
                 showEndDialogue()
                 */
            } else {
                val piece = get(s)
                if (piece?.isKing() == true) {
                    val atterkerPiece = get(attackingPosition)
                    movePiece(s, attackingPosition, piece)
                    val ss = findAttacker(attackingPosition, opsiteTurn)
                    reverseMove(s, attackingPosition, piece, atterkerPiece)
                    Log.d("Jjj", "$ss")

                    if (ss != -1) {
                        showEndDialogue("Check Mate!")
                    }
                }
            }

            Log.d("Jjj", "attackingPosition: $attackingPosition, s = $s")
        }

        // Special handling for Rook and King castling moves
        if (isRook) {
            when (fromIndex) {
                0 -> whiteCastling.markFirstRookMoved()  // Mark white's first rook as moved
                7 -> whiteCastling.markSecondRookMoved()  // Mark white's second rook as moved
                56 -> blackCastling.markFirstRookMoved()  // Mark black's first rook as moved
                63 -> blackCastling.markSecondRookMoved()  // Mark black's second rook as moved
            }
        } else if (isKing) {
            if (isWhiteTurn) whiteCastling.markKingMoved() else blackCastling.markKingMoved()  // Mark the king as moved for castling
        } else if (fromPiece.isPawn() && pawnCanRevive(toIndex)) {
            revivePawn(toIndex)  // Check if the pawn can be promoted to a new piece (e.g., Queen)
        }

        // Handle capture or checkmate scenarios
        if (toPiece != null) {
            if (toPiece.isKing()) {
                showEndDialogue()  // If the captured piece is a king, show the end dialog (checkmate)
                return false
            } else {
                message("${fromPiece.symbol} attacks and captures ${toPiece.symbol}")  // Inform the player of the capture
            }
        }

        // Switch turns after the move
        isWhiteTurn = !isWhiteTurn
        sendData()
        return true
    }


    /**
     * Checks whether the given side is in check by any opponent piece.
     *
     * @param isWhitePiece True if checking for the white king, false for black king.
     * @return True if the king is under threat (check), false otherwise.
     */
    private fun isCheck(isWhitePiece: Boolean): Int {
        // Find the position of the king of the current side
        val kingPosition = chessBoard.indexOfFirst {
            it != null && it.isKing() && it.color == if (isWhitePiece) pieceLightColor else pieceDarkColor
        }

        // If the king is not found (should not happen in a normal game), end the game
        if (kingPosition == -1) {
            showEndDialogue()
            return kingPosition
        }


        // Iterate over all opponent pieces to see if any can attack the king's position


        return findAttacker(kingPosition, isWhitePiece) // No threat found
    }

    fun findAttacker(movePosition: Int, isWhitePiece: Boolean): Int {
        for (position in indices) {
            val piece = chessBoard[position]
            if (piece == null || isFriend(piece, isWhitePiece)) continue

            // Check if the opponent's piece can move to the king's position
            if ((piece.isPawn() && isPawnMoveAllowed(
                    position, movePosition, !isWhitePiece
                )) || (piece.isKnight() && isKnightMoveAllowed(
                    position, movePosition, !isWhitePiece
                )) || (piece.isBishop() && isBishopMoveAllowed(
                    position, movePosition, !isWhitePiece
                )) || (piece.isRook() && isRookMoveAllowed(
                    position, movePosition, !isWhitePiece
                )) || (piece.isQueen() && isQueenMoveAllowed(
                    position, movePosition, !isWhitePiece
                )) || (piece.isKing() && isKingMoveAllowed(
                    position, movePosition, 2, !isWhitePiece
                ))
            ) {
                return position // King is in check
            }
        }
        return -1
    }


    /**
     * Displays a short Toast message on the screen.
     *
     * @param message The message to show in the toast
     */
    private fun message(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }


    /**
     * Checks if a move is allowed in any of the 8 directions (horizontal, vertical, diagonal).
     * This function is used by Queen, Rook, Bishop, and King.
     *
     * @param from            The current position (0 to 63)
     * @param to              The target position
     * @param isWhitePiece    True if the piece is white
     * @param sequence        Max number of steps (e.g., 1 for King, 8 for Queen)
     * @param horizontal      Allow left-right movement
     * @param vertical        Allow up-down movement
     * @param diagonal        Allow diagonal movement
     *
     * @return true if the move is valid, false otherwise
     */
    private fun isMoveAllowed(
        from: Int,
        to: Int,
        isWhitePiece: Boolean,
        sequence: Int,
        horizontal: Boolean,
        vertical: Boolean,
        diagonal: Boolean
    ): Boolean {

        if (from == to) return false  // Can't move to the same square

        // Direction flags – turn false when path is blocked
        var left = horizontal
        var right = horizontal
        var up = vertical
        var down = vertical
        var upLeft = diagonal
        var upRight = diagonal
        var downLeft = diagonal
        var downRight = diagonal

        // Left-right limits to avoid wrapping across the board
        val leftLimit = from - (from % 8)
        val rightLimit = leftLimit + 8

        // Check all 8 directions up to the sequence length
        for (i in 1 until sequence) {

            // ← LEFT
            if (left) {
                val pos = from - i
                if (pos >= leftLimit) {
                    if (pos == to) return isValidMove(pos, isWhitePiece)
                    left = get(pos) == null  // Blocked if piece exists
                } else left = false
            }

            // → RIGHT
            if (right) {
                val pos = from + i
                if (pos < rightLimit) {
                    if (pos == to) return isValidMove(pos, isWhitePiece)
                    right = get(pos) == null
                } else right = false
            }

            // ↑ UP (increase index by 8 per row)
            if (up) {
                val pos = from + 8 * i
                if (pos <= 63) {
                    if (pos == to) return isValidMove(pos, isWhitePiece)
                    up = get(pos) == null
                } else up = false
            }

            // ↓ DOWN (decrease index by 8 per row)
            if (down) {
                val pos = from - 8 * i
                if (pos >= 0) {
                    if (pos == to) return isValidMove(pos, isWhitePiece)
                    down = get(pos) == null
                } else down = false
            }

            // ↖ UP-LEFT (7 steps per move)
            if (upLeft) {
                val pos = from + 7 * i
                if (pos <= 63 && pos % 8 != 7) {
                    if (pos == to) return isValidMove(pos, isWhitePiece)
                    upLeft = get(pos) == null
                } else upLeft = false
            }

            // ↗ UP-RIGHT (9 steps per move)
            if (upRight) {
                val pos = from + 9 * i
                if (pos <= 63 && pos % 8 != 0) {
                    if (pos == to) return isValidMove(pos, isWhitePiece)
                    upRight = get(pos) == null
                } else upRight = false
            }

            // ↙ DOWN-LEFT (-9 steps per move)
            if (downLeft) {
                val pos = from - 9 * i
                if (pos >= 0 && pos % 8 != 7) {
                    if (pos == to) return isValidMove(pos, isWhitePiece)
                    downLeft = get(pos) == null
                } else downLeft = false
            }

            // ↘ DOWN-RIGHT (-7 steps per move)
            if (downRight) {
                val pos = from - 7 * i
                if (pos >= 0 && pos % 8 != 0) {
                    if (pos == to) return isValidMove(pos, isWhitePiece)
                    downRight = get(pos) == null
                } else downRight = false
            }
        }

        // If none of the paths reach the target, return false
        return false
    }


    /**
     * Checks if the King’s move is valid.
     *
     * @param from The starting index of the King.
     * @param to The target index the King wants to move to.
     * @param sequence Custom movement sequence (typically unused for King but passed for flexibility).
     * @param isWhitePiece True if the King is white, false if black.
     * @return True if the move is valid, false otherwise.
     */
    private fun isKingMoveAllowed(
        from: Int, to: Int, sequence: Int, isWhitePiece: Boolean
    ): Boolean {
        return isMoveAllowed(
            from = from,
            to = to,
            isWhitePiece = isWhitePiece,
            sequence = sequence,
            horizontal = true,
            vertical = true,
            diagonal = true
        )
    }

    /**
     * Checks if the Queen’s move is valid.
     *
     * Queen can move horizontally, vertically, or diagonally any number of squares.
     *
     * @param from Starting square index.
     * @param to Target square index.
     * @param isWhitePiece True if the Queen is white.
     * @return True if the move is valid, false otherwise.
     */
    private fun isQueenMoveAllowed(from: Int, to: Int, isWhitePiece: Boolean): Boolean {
        return isMoveAllowed(
            from = from,
            to = to,
            isWhitePiece = isWhitePiece,
            sequence = 8, // Queen moves in any direction, so 8 is commonly used as base step
            horizontal = true,
            vertical = true,
            diagonal = true
        )
    }


    /**
     * Validates a Rook's move.
     *
     * Rook moves horizontally or vertically across the board.
     *
     * @param from Starting position index.
     * @param to Destination position index.
     * @param isWhitePiece Whether the piece is white.
     * @return True if the move is legal for a Rook.
     */
    private fun isRookMoveAllowed(from: Int, to: Int, isWhitePiece: Boolean): Boolean {
        return isMoveAllowed(
            from = from,
            to = to,
            isWhitePiece = isWhitePiece,
            sequence = 8, // Step size for linear movement
            horizontal = true,
            vertical = true,
            diagonal = false
        )
    }


    /**
     * Validates a Bishop's move.
     *
     * Bishop moves only diagonally on the board.
     *
     * @param from Starting position index.
     * @param to Destination position index.
     * @param isWhitePiece Whether the piece is white.
     * @return True if the move is legal for a Bishop.
     */
    private fun isBishopMoveAllowed(from: Int, to: Int, isWhitePiece: Boolean): Boolean {
        return isMoveAllowed(
            from = from,
            to = to,
            isWhitePiece = isWhitePiece,
            sequence = 8, // Step size, reused for diagonal logic
            horizontal = false,
            vertical = false,
            diagonal = true
        )
    }


    /**
     * Checks if a knight's move is allowed based on chess rules.
     *
     * @param from The starting position of the knight.
     * @param to The target position to move to.
     * @param isWhitePiece Indicates whether the piece is a white piece or not.
     * @return True if the move is valid for the knight, false otherwise.
     */
    private fun isKnightMoveAllowed(
        from: Int, to: Int, isWhitePiece: Boolean
    ): Boolean {
        if (from == to) return false

        val sequence = intArrayOf(
            from + 6, from - 6, from + 10, from - 10, from + 15, from - 15, from + 17, from - 17
        )

        return to in sequence && isValidMove(to, isWhitePiece)
    }


    /**
     * Checks if a pawn's move is valid.
     *
     * @param from Starting position of the pawn.
     * @param to Target position of the pawn.
     * @param isWhitePiece True if the pawn is white, false if black.
     * @return True if the pawn can move to the target square, false otherwise.
     */
    private fun isPawnMoveAllowed(from: Int, to: Int, isWhitePiece: Boolean): Boolean {
        if (from == to) return false

        val forwardStep = if (isWhitePiece) 8 else -8
        val doubleForwardStep = if (isWhitePiece) 16 else -16
        val captureLeft = if (isWhitePiece) 7 else -7
        val captureRight = if (isWhitePiece) 9 else -9

        // Diagonal captures
        if (to == from + captureLeft || to == from + captureRight) {
            val piece = get(to)
            return piece != null && !isFriend(piece, isWhitePiece)
        }

        // One step forward
        if (to == from + forwardStep) {
            return get(to) == null
        }

        // Two steps forward from starting rank
        val isStartingRank = if (isWhitePiece) from in 8..15 else from in 48..55
        if (isStartingRank && to == from + doubleForwardStep) {
            return get(to) == null
        }

        return false
    }


    /**
     * Transforms a chess piece symbol to its opposite color counterpart.
     *
     * @param symbol The chess piece symbol to transform.
     * @return The transformed symbol, or the original symbol if no transformation is needed.
     */
    fun transform(symbol: String): String {
        return when (symbol) {
            "♙" -> "♟"
            "♖" -> "♜"
            "♘" -> "♞"
            "♗" -> "♝"
            "♕" -> "♛"
            "♔" -> "♚"

            "♟" -> "♙"
            "♜" -> "♖"
            "♞" -> "♘"
            "♝" -> "♗"
            "♛" -> "♕"
            "♚" -> "♔"

            else -> symbol
        }
    }


    /**
     * Creates a set of chess pieces based on the given symbols and color.
     *
     * @param symbols Array of symbols representing the pieces.
     * @param color The color of the pieces (either light or dark).
     * @return Array of Piece objects created with the given symbols and color.
     */
    private fun createPieces(symbols: Array<String>, color: Int): Array<Piece> {
        return Array(symbols.size) { i -> Piece(symbols[i], color) }
    }

    /**
     * Shows a game over dialog with the option to restart the game.
     */
    private fun showEndDialogue(description: String? = null) {
        MaterialAlertDialogBuilder(context).setTitle("Game Over").setMessage(description)
            .setPositiveButton("Restart") { _, _ -> (context as Activity).recreate() }
            .setCancelable(false).show()
    }

    /**
     * Checks if king-side castling is available for the current player (either white or black).
     * King-side castling is available if:
     * - The king and the rook have not moved yet.
     * - The squares between the king and the rook are not occupied.
     * - The king is not in check, does not pass through check, and does not end up in check.
     *
     * @param isWhitePiece A boolean indicating whether the piece being checked is white.
     *                     `true` for white pieces, `false` for black pieces.
     * @return `true` if king-side castling is available for the current player,
     *         `false` otherwise.
     */
    private fun kingSideCastleAvailable(isWhitePiece: Boolean): Boolean {
        return if (isWhiteTurn) {
            // Check if king-side castling is available for white and if the king's move is allowed
            whiteCastling.isKingSideCastlingPossible() && isKingMoveAllowed(4, 6, 3, isWhitePiece)
        } else {
            // Check if king-side castling is available for black and if the king's move is allowed
            blackCastling.isKingSideCastlingPossible() && isKingMoveAllowed(60, 62, 3, isWhitePiece)
        }
    }


    /**
     * Checks if queen-side castling is available for the current player (either white or black).
     * Queen-side castling is available if:
     * - The king and the rook have not moved yet.
     * - The squares between the king and the rook are not occupied.
     * - The king is not in check, does not pass through check, and does not end up in check.
     *
     * @param isWhitePiece A boolean indicating whether the piece being checked is white.
     *                     `true` for white pieces, `false` for black pieces.
     * @return `true` if queen-side castling is available for the current player,
     *         `false` otherwise.
     */
    private fun queenSideCastleAvailable(isWhitePiece: Boolean): Boolean {
        return if (isWhiteTurn) {
            // Check if queen-side castling is available for white and if the king's move is allowed
            whiteCastling.isQueenSideCastlingPossible() && isKingMoveAllowed(4, 2, 3, isWhitePiece)
        } else {
            // Check if queen-side castling is available for black and if the king's move is allowed
            blackCastling.isQueenSideCastlingPossible() && isKingMoveAllowed(
                60, 58, 3, isWhitePiece
            )
        }
    }


    /**
     * Checks if a castling move is possible for the current player (either white or black).
     * Castling involves moving the king two squares toward a rook and then moving the rook
     * to the square the king has jumped over. Castling is only available if:
     * - The king and the rook have not moved previously.
     * - The squares between the king and rook are not occupied.
     * - The king is not currently in check, does not move through check, and does not end up in check.
     *
     * @param fromIndex The starting position of the piece attempting to move.
     * @param toIndex The target position of the piece after the move.
     * @param isFromWhitePiece A boolean indicating whether the piece being moved is white.
     *                         `true` for white, `false` for black.
     * @param fromPiece The piece that is being moved from `fromIndex`.
     * @param toPiece The piece that is being moved to `toIndex` (could be `null` if empty).
     * @return `true` if castling is successful, `false` otherwise.
     */
    private fun isCastled(
        fromIndex: Int, toIndex: Int, isFromWhitePiece: Boolean, fromPiece: Piece, toPiece: Piece?
    ): Boolean {
        val rookFrom: Int
        val rookTo: Int


        // Check if castling is attempted (either king-side or queen-side)
        if (if (isWhiteTurn) fromIndex == 4 && (toIndex == 2 || toIndex == 6) else fromIndex == 60 && (toIndex == 58 || toIndex == 62)) {

            when (toIndex) {
                2 -> if (queenSideCastleAvailable(isFromWhitePiece)) {
                    rookFrom = 0
                    rookTo = 3
                } else return false

                6 -> if (kingSideCastleAvailable(isFromWhitePiece)) {
                    rookFrom = 7
                    rookTo = 5
                } else return false

                58 -> if (queenSideCastleAvailable(isFromWhitePiece)) {
                    rookFrom = 56
                    rookTo = 59
                } else return false

                62 -> if (kingSideCastleAvailable(isFromWhitePiece)) {
                    rookFrom = 63
                    rookTo = 61
                } else return false

                else -> return false
            }

            // Ensure the rook at the starting position is a valid rook
            val fromRookPiece = chessBoard[rookFrom]
            if (fromRookPiece == null || !fromRookPiece.isRook()) return false

            // Check if the king is in check before making the move
            if (isCheck(isFromWhitePiece) != -1) return false

            // Move both the king and the rook
            movePiece(fromIndex, toIndex, fromPiece)
            movePiece(rookFrom, rookTo, fromRookPiece)

            // Check if the king would be in check after castling
            if (isCheck(isFromWhitePiece) != -1) {
                // Reverse the move if the king is in check
                reverseMove(rookFrom, rookTo, fromRookPiece, null)
                reverseMove(fromIndex, toIndex, fromPiece, toPiece)
                message("Illegal move: Your King would be in check.")
                return false
            }

            // Mark the castling move as completed
            if (isFromWhitePiece) whiteCastling.markCastled()
            else blackCastling.markCastled()

            message("The King has castled.")
            return true
        }

        return false
    }


    /**
     * Reverses a move by swapping the pieces on the chessboard.
     *
     * @param fromIndex The position to move the piece from.
     * @param toIndex The position to move the piece to.
     * @param fromPiece The piece to place at the `fromIndex`.
     * @param toPiece The piece to place at the `toIndex`, could be null.
     */
    private fun reverseMove(fromIndex: Int, toIndex: Int, fromPiece: Piece, toPiece: Piece?) {
        chessBoard[fromIndex] = fromPiece
        chessBoard[toIndex] = toPiece
    }

    /**
     * Checks if the pawn at the given position can be revived.
     *
     * @param position The position of the pawn to check.
     * @return True if the pawn can be revived, false otherwise.
     */
    private fun pawnCanRevive(position: Int): Boolean {
        return (if (isWhiteTurn) position in 56..63 else position in 0..7) && chessBoard[position]?.isPawn() == true
    }

    /**
     * Shows a dialog to revive a pawn into another chess piece.
     * Updates the board and invokes post-revival logic.
     *
     * @param position The position of the pawn to be revived.
     */
    private fun revivePawn(position: Int) {
        val symbols = if ((if (isWhiteTurn) isLightFilled else isDarkFilled)) arrayOf(
            "♛", "♜", "♝", "♞"
        ) // White turn, light theme
        else arrayOf("♕", "♖", "♗", "♘") // Black turn, dark theme

        var selected = 0

        MaterialAlertDialogBuilder(context).setTitle("Revive Your Pawn")
            .setSingleChoiceItems(symbols, selected) { _, which ->
                selected = which
            }.setPositiveButton("Okay") { _, _ ->
                val symbol = symbols[selected]

                val piece = chessBoard[position]
                if (piece != null) {
                    piece.symbol = symbol
                    chessBoard[position] = piece
                }

                afterRevival()
                message("The Pawn revived to $symbol")
            }.setCancelable(false).show()
    }


    /**
     * Sets a callback function to be executed after a revival action.
     * This function allows the game logic to define what should happen after
     * a specific revival action (such as when a pawn reaches the promotion rank
     * and is revived into another piece).
     *
     * @param afterRevival A callback function that will be executed after the revival action.
     */
    fun setAfterRevival(afterRevival: () -> Unit) {
        this.afterRevival = afterRevival
    }

    fun setOnlinePlayer(onlinePlayer: (String, String) -> Unit) {
        this.onlinePlayer = onlinePlayer
        this.playingOnline = true

        sendData()
    }

    fun sendData() {
        if (fromOnline()) {
            val friends = StringBuilder()
            val enemies = StringBuilder()
            for (position in indices) {
                val piece = chessBoard[position]
                if (isFriend(piece, true)) {
                    friends.append(position).append("position : ").append(piece!!.symbol)
                        .append(",\n")
                } else if (isFriend(piece, false)) {
                    enemies.append(position).append("position : ").append(piece!!.symbol)
                        .append(",\n")
                }
            }
            onlinePlayer?.invoke(friends.toString(), enemies.toString())
        }
    }

    fun fromOnline(): Boolean {
        return isWhiteTurn && playingOnline
    }


}

