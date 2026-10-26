"""Portable locations for locally cached Minecraft reference files."""
import os
from pathlib import Path


def minecraft_reference(name='client.jar'):
    explicit = os.environ.get('XIANTU_MINECRAFT_REFERENCE_DIR')
    homes = [Path(os.environ.get('GRADLE_USER_HOME', Path.home()/'.gradle'))]
    if os.name == 'nt':
        homes.append(Path('C:/gradle-user'))
    paths = ([Path(explicit)/name] if explicit else []) + [
        home/'caches/forge_gradle/minecraft_repo/versions/1.20.1'/name for home in homes]
    for path in paths:
        if path.is_file():
            return path
    raise FileNotFoundError(f'{name}: run Gradle dependency preparation or set XIANTU_MINECRAFT_REFERENCE_DIR')


def review_font(size):
    from PIL import ImageFont
    try:
        return ImageFont.truetype(os.environ.get('XIANTU_REVIEW_FONT', 'C:/Windows/Fonts/msyh.ttc'), size)
    except OSError:
        return ImageFont.load_default()
