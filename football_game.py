"""A small two-player football game made with pygame.

Install pygame with: python -m pip install pygame
Run with: python football_game.py
"""

import sys

import pygame


WIDTH, HEIGHT = 960, 540
FPS = 60
FIELD = (34, 139, 82)
WHITE = (245, 245, 235)
RED = (218, 65, 65)
BLUE = (65, 110, 218)
YELLOW = (246, 205, 70)


def reset_ball():
    return pygame.Vector2(WIDTH / 2, HEIGHT / 2)


def main():
    pygame.init()
    screen = pygame.display.set_mode((WIDTH, HEIGHT))
    pygame.display.set_caption("Football Game")
    clock = pygame.time.Clock()
    font = pygame.font.Font(None, 42)

    player_one = pygame.Vector2(180, HEIGHT / 2)
    player_two = pygame.Vector2(WIDTH - 180, HEIGHT / 2)
    ball = reset_ball()
    velocity = pygame.Vector2(0, 0)
    score = [0, 0]
    running = True

    while running:
        for event in pygame.event.get():
            if event.type == pygame.QUIT:
                running = False
            if event.type == pygame.KEYDOWN and event.key == pygame.K_ESCAPE:
                running = False

        keys = pygame.key.get_pressed()
        first_move = pygame.Vector2(
            keys[pygame.K_d] - keys[pygame.K_a],
            keys[pygame.K_s] - keys[pygame.K_w],
        )
        second_move = pygame.Vector2(
            keys[pygame.K_RIGHT] - keys[pygame.K_LEFT],
            keys[pygame.K_DOWN] - keys[pygame.K_UP],
        )

        for player, movement in ((player_one, first_move), (player_two, second_move)):
            if movement.length_squared():
                player += movement.normalize() * 5
            player.x = max(35, min(WIDTH - 35, player.x))
            player.y = max(75, min(HEIGHT - 35, player.y))

            if player.distance_to(ball) < 38:
                direction = ball - player
                if not direction.length_squared():
                    direction = pygame.Vector2(1, 0)
                velocity = direction.normalize() * 9

        ball += velocity
        velocity *= 0.985

        if ball.y < 62 or ball.y > HEIGHT - 22:
            velocity.y *= -1
            ball.y = max(62, min(HEIGHT - 22, ball.y))

        if ball.x < 0:
            score[1] += 1
            ball = reset_ball()
            velocity.update(0, 0)
        elif ball.x > WIDTH:
            score[0] += 1
            ball = reset_ball()
            velocity.update(0, 0)

        screen.fill(FIELD)
        pygame.draw.rect(screen, WHITE, (0, 55, WIDTH, HEIGHT - 55), 4)
        pygame.draw.line(screen, WHITE, (WIDTH / 2, 55), (WIDTH / 2, HEIGHT), 3)
        pygame.draw.circle(screen, WHITE, (WIDTH / 2, HEIGHT / 2), 75, 3)
        pygame.draw.rect(screen, WHITE, (0, HEIGHT / 2 - 95, 85, 190), 3)
        pygame.draw.rect(screen, WHITE, (WIDTH - 85, HEIGHT / 2 - 95, 85, 190), 3)
        pygame.draw.circle(screen, YELLOW, ball, 14)
        pygame.draw.circle(screen, RED, player_one, 25)
        pygame.draw.circle(screen, BLUE, player_two, 25)

        scoreboard = font.render(f"RED {score[0]}   -   {score[1]} BLUE", True, WHITE)
        screen.blit(scoreboard, scoreboard.get_rect(center=(WIDTH / 2, 28)))
        pygame.display.flip()
        clock.tick(FPS)

    pygame.quit()
    sys.exit()


if __name__ == "__main__":
    main()