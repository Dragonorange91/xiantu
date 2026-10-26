from pathlib import Path
import argparse

from PIL import Image


def extend(source_path: Path, output_path: Path) -> None:
    source = Image.open(source_path).convert("RGBA")
    if source.size != (256, 256):
        raise ValueError(f"Expected 256x256 source texture, got {source.size}")

    canvas = Image.new("RGBA", (256, 400), (0, 0, 0, 0))
    canvas.paste(source.crop((0, 0, 256, 153)), (0, 0))

    # Preserve the panel's top and bottom ornament bands while adding vertical room.
    panel = source.crop((0, 153, 256, 226))
    border = 7
    middle = panel.crop((0, border, 256, panel.height - border))
    details_height = 144
    middle = middle.resize((256, details_height - border * 2), Image.Resampling.NEAREST)
    expanded_details = Image.new("RGBA", (256, details_height), (0, 0, 0, 0))
    expanded_details.paste(panel.crop((0, 0, 256, border)), (0, 0))
    expanded_details.paste(middle, (0, border))
    expanded_details.paste(panel.crop((0, panel.height - border, 256, panel.height)),
                            (0, details_height - border))
    canvas.paste(expanded_details, (0, 153))

    # Reuse the matching full-width details frame as the future profession panel.
    canvas.paste(panel, (0, 153 + details_height))
    canvas.paste(source.crop((0, 226, 256, 256)), (0, 153 + details_height + panel.height))

    output_path.parent.mkdir(parents=True, exist_ok=True)
    canvas.save(output_path)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("source", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()
    extend(args.source, args.output)


if __name__ == "__main__":
    main()
