from pathlib import Path
import subprocess
import sys

from ragas_evaluation.cli import main


PROJECT_ROOT = Path(__file__).parents[1]


def test_project_declares_supported_python_and_exactly_pinned_dependencies():
    pyproject = (PROJECT_ROOT / "pyproject.toml").read_text(encoding="utf-8")
    lockfile = (PROJECT_ROOT / "requirements.lock").read_text(encoding="utf-8")

    assert "requires-python = \">=3.10,<3.11\"" in pyproject
    dependency_lines = [
        line.strip()
        for line in lockfile.splitlines()
        if line.strip() and not line.lstrip().startswith("#")
    ]
    assert dependency_lines
    assert all("==" in line for line in dependency_lines)
    assert any(line.startswith("ragas==") for line in dependency_lines)
    assert any(line.startswith("pytest==") for line in dependency_lines)


def test_cli_help_is_available_from_the_single_entry_point(capsys):
    assert main(["--help"]) == 0

    output = capsys.readouterr().out
    assert "offline retrieval-context evaluation" in output
    assert "--dataset" in output
    assert "--evidence" in output
    assert "--output-dir" in output


def test_module_entry_point_displays_cli_help():
    result = subprocess.run(
        [sys.executable, "-m", "ragas_evaluation", "--help"],
        cwd=PROJECT_ROOT,
        capture_output=True,
        text=True,
        check=False,
    )

    assert result.returncode == 0
    assert "offline retrieval-context evaluation" in result.stdout
